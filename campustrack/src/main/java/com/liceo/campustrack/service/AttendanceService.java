package com.liceo.campustrack.service;

import com.liceo.campustrack.datastructure.DynamicArray;
import com.liceo.campustrack.datastructure.HashTable;
import com.liceo.campustrack.datastructure.LinkedQueue;
import com.liceo.campustrack.datastructure.Sorting;
import com.liceo.campustrack.model.AttendanceRecord;
import com.liceo.campustrack.model.AttendanceStatus;
import com.liceo.campustrack.model.ClassSession;
import com.liceo.campustrack.model.CourseClass;
import com.liceo.campustrack.model.Role;
import com.liceo.campustrack.model.User;
import com.liceo.campustrack.repository.AttendanceRepository;
import com.liceo.campustrack.repository.ClassSessionRepository;
import com.liceo.campustrack.repository.CourseClassRepository;
import com.liceo.campustrack.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Comparator;

@Service
public class AttendanceService {

    /** One line of the faculty live feed. */
    public record AttendanceRow(Long studentId, String name, String idNumber, String status, String time, String method) {}

    /** A student's attendance summary for one subject. */
    public record SubjectStat(String code, String name, int present, int late, int absent, int total, int percent) {}

    /** Shown in the student's success pop-up. */
    public record CheckInResult(String className, String time, String status) {}

    /** Outcome of one queued offline scan. */
    public record SyncResult(String qrPayload, boolean success, String message) {}

    private static final long DEBOUNCE_MILLIS = 10_000;
    private static final int MAX_SYNC_ITEMS = 50;
    private static final int OFFLINE_SYNC_MAX_AGE_HOURS = 12;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("h:mm:ss a");
    private static final DateTimeFormatter CSV_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter CSV_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final AttendanceRepository records;
    private final ClassSessionRepository sessions;
    private final CourseClassRepository classes;
    private final UserRepository users;
    private final QrCodeService qr;
    private final AuditService audit;
    private final int lateAfterMinutes;
    private final SecureRandom random = new SecureRandom();

    // Debounce memory: the last code each student submitted, and when.
    private final HashTable<Long, String> lastPayload = new HashTable<>();
    private final HashTable<Long, Long> lastScanMillis = new HashTable<>();

    public AttendanceService(AttendanceRepository records, ClassSessionRepository sessions,
                             CourseClassRepository classes, UserRepository users,
                             QrCodeService qr, AuditService audit,
                             @Value("${campustrack.attendance.late-after-minutes:15}") int lateAfterMinutes) {
        this.records = records;
        this.sessions = sessions;
        this.classes = classes;
        this.users = users;
        this.qr = qr;
        this.audit = audit;
        this.lateAfterMinutes = lateAfterMinutes;
    }

    // ---------------------------------------------------------------- classes

    /** A faculty member's classes, sorted by subject code. */
    public DynamicArray<CourseClass> classesOf(User faculty) {
        return Sorting.mergeSort(
                classes.findWhere(c -> faculty.getId().equals(c.getFacultyId())),
                Comparator.comparing(CourseClass::getCode, String.CASE_INSENSITIVE_ORDER));
    }

    public DynamicArray<CourseClass> allClasses() {
        return Sorting.mergeSort(classes.findAll(), Comparator.comparing(CourseClass::getCode, String.CASE_INSENSITIVE_ORDER));
    }

    public synchronized CourseClass createClass(User faculty, String code, String name, String schedule) {
        code = clean(code);
        name = clean(name);
        if (code.isEmpty() || code.length() > 30) throw new IllegalArgumentException("Subject code is required (up to 30 characters)");
        if (name.isEmpty() || name.length() > 100) throw new IllegalArgumentException("Subject name is required (up to 100 characters)");

        CourseClass courseClass = new CourseClass();
        courseClass.setCode(code);
        courseClass.setName(name);
        courseClass.setFacultyId(faculty.getId());
        courseClass.setSchedule(clean(schedule));
        classes.save(courseClass);
        audit.log(faculty, "CLASS_CREATE", courseClass.getLabel());
        return courseClass;
    }

    public synchronized void updateClass(User faculty, Long classId, String code, String name, String schedule) {
        CourseClass courseClass = ownedClass(faculty, classId);
        code = clean(code);
        name = clean(name);
        if (code.isEmpty() || code.length() > 30) throw new IllegalArgumentException("Subject code is required (up to 30 characters)");
        if (name.isEmpty() || name.length() > 100) throw new IllegalArgumentException("Subject name is required (up to 100 characters)");
        courseClass.setCode(code);
        courseClass.setName(name);
        courseClass.setSchedule(clean(schedule));
        classes.save(courseClass);
        audit.log(faculty, "CLASS_UPDATE", courseClass.getLabel());
    }

    /** Deletes the class together with its sessions and attendance records. */
    public synchronized void deleteClass(User faculty, Long classId) {
        CourseClass courseClass = ownedClass(faculty, classId);
        int removedRecords = records.deleteWhere(r -> classId.equals(r.getClassId()));
        int removedSessions = sessions.deleteWhere(s -> classId.equals(s.getClassId()));
        classes.deleteById(classId);
        audit.log(faculty, "CLASS_DELETE", courseClass.getLabel() + " (" + removedSessions + " session(s), "
                + removedRecords + " attendance record(s) removed)");
    }

    /** Enrolls students by ID number. Accepts several numbers separated by commas, spaces or new lines. */
    public synchronized String enroll(User faculty, Long classId, String idNumbers) {
        CourseClass courseClass = ownedClass(faculty, classId);
        int added = 0;
        StringBuilder unknown = new StringBuilder();
        for (String idNumber : clean(idNumbers).split("[,;\\s]+")) {
            if (idNumber.isEmpty()) continue;
            User student = users.findByIdNumber(idNumber);
            if (student == null || student.getRole() != Role.STUDENT) {
                unknown.append(unknown.length() == 0 ? "" : ", ").append(idNumber);
            } else if (courseClass.getStudentIds().add(student.getId())) {
                added++;
            }
        }
        if (added > 0) {
            classes.save(courseClass);
            audit.log(faculty, "CLASS_ENROLL", added + " student(s) added to " + courseClass.getCode());
        }
        String message = added + " student(s) enrolled in " + courseClass.getCode() + ".";
        return unknown.length() == 0 ? message : message + " No student account found for: " + unknown;
    }

    public synchronized void unenroll(User faculty, Long classId, Long studentId) {
        CourseClass courseClass = ownedClass(faculty, classId);
        if (courseClass.getStudentIds().remove(studentId)) {
            classes.save(courseClass);
            audit.log(faculty, "CLASS_UNENROLL", "Student " + studentId + " removed from " + courseClass.getCode());
        }
    }

    public CourseClass ownedClass(User faculty, Long classId) {
        CourseClass courseClass = classes.findById(classId);
        if (courseClass == null || !faculty.getId().equals(courseClass.getFacultyId())) {
            throw new IllegalArgumentException("Class not found");
        }
        return courseClass;
    }

    /** Enrolled students of a class, sorted by name. */
    public DynamicArray<User> studentsOf(CourseClass courseClass) {
        DynamicArray<User> students = new DynamicArray<>();
        for (Long studentId : courseClass.getStudentIds().values()) {
            User student = users.findById(studentId);
            if (student != null) students.add(student);
        }
        return Sorting.mergeSort(students, Comparator.comparing(User::getFullName, String.CASE_INSENSITIVE_ORDER));
    }

    // --------------------------------------------------------------- sessions

    public synchronized ClassSession startSession(User faculty, Long classId) {
        CourseClass courseClass = ownedClass(faculty, classId);
        // Only one live session per class
        for (ClassSession running : sessions.findWhere(s -> s.isActive() && s.getClassId().equals(classId))) {
            endSession(faculty, running.getId());
        }

        byte[] secret = new byte[32];
        random.nextBytes(secret);

        ClassSession session = new ClassSession();
        session.setClassId(classId);
        session.setFacultyId(faculty.getId());
        session.setSecret(Base64.getUrlEncoder().withoutPadding().encodeToString(secret));
        session.setStartedAt(LocalDateTime.now());
        sessions.save(session);
        audit.log(faculty, "SESSION_START", courseClass.getLabel());
        return session;
    }

    /** Closes the session and marks every enrolled student who never checked in as ABSENT. */
    public synchronized void endSession(User faculty, Long sessionId) {
        ClassSession session = ownedSession(faculty, sessionId);
        if (!session.isActive()) return;
        session.setEndedAt(LocalDateTime.now());
        sessions.save(session);

        CourseClass courseClass = classes.findById(session.getClassId());
        int absent = 0;
        if (courseClass != null) {
            for (Long studentId : courseClass.getStudentIds().values()) {
                if (records.findBySessionAndUser(sessionId, studentId) == null) {
                    records.save(newRecord(session, studentId, AttendanceStatus.ABSENT, session.getEndedAt(), "AUTO"));
                    absent++;
                }
            }
        }
        audit.log(faculty, "SESSION_END", "Session " + sessionId + " closed, " + absent + " marked absent");
    }

    public ClassSession ownedSession(User faculty, Long sessionId) {
        ClassSession session = sessions.findById(sessionId);
        if (session == null || !faculty.getId().equals(session.getFacultyId())) {
            throw new IllegalArgumentException("Class session not found");
        }
        return session;
    }

    public DynamicArray<ClassSession> activeSessionsOf(User faculty) {
        return sessions.findWhere(s -> s.isActive() && faculty.getId().equals(s.getFacultyId()));
    }

    public DynamicArray<ClassSession> recentSessionsOf(User faculty, int max) {
        DynamicArray<ClassSession> ended = sessions.findWhere(s -> !s.isActive() && faculty.getId().equals(s.getFacultyId()));
        return Sorting.mergeSort(ended, Comparator.comparing(ClassSession::getStartedAt).reversed()).limit(max);
    }

    public int activeSessionCount() {
        return sessions.findWhere(ClassSession::isActive).size();
    }

    /** Every enrolled student with their current status. "WAITING" means not checked in yet. */
    public DynamicArray<AttendanceRow> liveRows(ClassSession session) {
        DynamicArray<AttendanceRow> rows = new DynamicArray<>();
        CourseClass courseClass = classes.findById(session.getClassId());
        if (courseClass == null) return rows;
        for (User student : studentsOf(courseClass)) {
            AttendanceRecord record = records.findBySessionAndUser(session.getId(), student.getId());
            rows.add(new AttendanceRow(student.getId(), student.getFullName(), student.getIdNumber(),
                    record == null ? "WAITING" : record.getStatus().name(),
                    record == null ? "" : record.getCheckInTime().format(CLOCK),
                    record == null ? "" : record.getEntryMethod()));
        }
        return rows;
    }

    /** Faculty sets a student's status by hand (dead phone battery, disputed scan, ...). */
    public synchronized void override(User faculty, Long sessionId, Long studentId, AttendanceStatus status) {
        ClassSession session = ownedSession(faculty, sessionId);
        CourseClass courseClass = classes.findById(session.getClassId());
        if (status == null) throw new IllegalArgumentException("Select a status");
        if (courseClass == null || !courseClass.isEnrolled(studentId)) {
            throw new IllegalArgumentException("That student is not enrolled in this class");
        }
        AttendanceRecord record = records.findBySessionAndUser(sessionId, studentId);
        if (record == null) record = newRecord(session, studentId, status, LocalDateTime.now(), "MANUAL_OVERRIDE");
        record.setStatus(status);
        record.setEntryMethod("MANUAL_OVERRIDE");
        record.setNote("Set by " + faculty.getFullName());
        records.save(record);
        audit.log(faculty, "ATTENDANCE_OVERRIDE", courseClass.getCode() + ": student " + studentId + " set to " + status);
    }

    // ------------------------------------------------------- student check-in

    /**
     * Records a check-in from a session code.
     *
     * @param offline true for scans replayed from the phone's offline queue. Those are
     *                validated against the time window stored inside the code itself.
     */
    public synchronized CheckInResult checkIn(User student, String payload, String method, boolean offline) {
        if (student.getRole() != Role.STUDENT) throw new IllegalStateException("Only student accounts can check in.");
        payload = clean(payload);
        if (payload.isEmpty()) throw new IllegalArgumentException("No QR code was provided.");
        debounce(student.getId(), payload);

        QrCodeService.Token token = qr.parse(payload);
        if (token == null) throw new IllegalArgumentException("That is not a CampusTrack class session code.");
        ClassSession session = sessions.findById(token.sessionId());
        if (session == null) throw new IllegalArgumentException("Class session not found.");

        long now = qr.currentWindow();
        LocalDateTime checkInTime;
        if (offline) {
            if (token.window() == null || token.window() > now || !qr.matches(session, token, token.window())) {
                throw new IllegalArgumentException("Invalid session code.");
            }
            checkInTime = qr.windowStart(token.window());
            LocalDateTime windowEnd = checkInTime.plusSeconds(QrCodeService.ROTATE_SECONDS);
            if (windowEnd.isBefore(session.getStartedAt())
                    || (session.getEndedAt() != null && checkInTime.isAfter(session.getEndedAt()))) {
                throw new IllegalArgumentException("That code was not shown during the class session.");
            }
            if (checkInTime.isBefore(LocalDateTime.now().minusHours(OFFLINE_SYNC_MAX_AGE_HOURS))) {
                throw new IllegalArgumentException("This offline scan is too old to sync. Ask your teacher for a manual check-in.");
            }
            if (checkInTime.isBefore(session.getStartedAt())) checkInTime = session.getStartedAt();
        } else {
            if (!session.isActive()) throw new IllegalStateException("This class session has already ended.");
            // Accept the code on screen and the one just before it (covers the moment of rotation)
            if (!qr.matches(session, token, now) && !qr.matches(session, token, now - 1)) {
                throw new IllegalArgumentException("Code is wrong or expired. Use the code that is on screen right now.");
            }
            checkInTime = LocalDateTime.now();
        }

        CourseClass courseClass = classes.findById(session.getClassId());
        if (courseClass == null || !courseClass.isEnrolled(student.getId())) {
            throw new IllegalStateException("You are not enrolled in "
                    + (courseClass == null ? "this class" : courseClass.getLabel()) + ".");
        }
        if (records.findBySessionAndUser(session.getId(), student.getId()) != null) {
            throw new IllegalStateException("You have already checked in for this class session.");
        }

        long minutesLate = Duration.between(session.getStartedAt(), checkInTime).toMinutes();
        AttendanceStatus status = minutesLate >= lateAfterMinutes ? AttendanceStatus.LATE : AttendanceStatus.PRESENT;
        records.save(newRecord(session, student.getId(), status, checkInTime, method));
        return new CheckInResult(courseClass.getLabel(), checkInTime.format(CLOCK), status.name());
    }

    /** Replays the phone's offline queue in the order the scans were made (FIFO). */
    public DynamicArray<SyncResult> syncOffline(User student, Iterable<String> payloads) {
        LinkedQueue<String> queue = new LinkedQueue<>();
        for (String payload : payloads) {
            if (queue.size() == MAX_SYNC_ITEMS) break;
            queue.enqueue(payload);
        }
        DynamicArray<SyncResult> results = new DynamicArray<>();
        while (!queue.isEmpty()) {
            String payload = queue.dequeue();
            try {
                CheckInResult result = checkIn(student, payload, "OFFLINE_SYNC", true);
                results.add(new SyncResult(payload, true, result.className() + " - " + result.status()));
            } catch (IllegalArgumentException | IllegalStateException e) {
                results.add(new SyncResult(payload, false, e.getMessage()));
            }
        }
        return results;
    }

    /** Blocks the same student sending the same code again within 10 seconds. */
    private void debounce(Long studentId, String payload) {
        long nowMillis = System.currentTimeMillis();
        Long previous = lastScanMillis.get(studentId);
        if (previous != null && nowMillis - previous < DEBOUNCE_MILLIS && payload.equals(lastPayload.get(studentId))) {
            throw new IllegalStateException("Duplicate scan ignored. Please wait a few seconds.");
        }
        lastScanMillis.put(studentId, nowMillis);
        lastPayload.put(studentId, payload);
    }

    public DynamicArray<AttendanceRecord> historyOf(Long userId) {
        return newestFirst(records.findWhere(r -> userId.equals(r.getUserId())));
    }

    /** Per-subject totals for a student. Attendance % = (present + late) / sessions held. */
    public DynamicArray<SubjectStat> subjectStats(Long userId) {
        HashTable<Long, int[]> counts = new HashTable<>(); // classId -> [present, late, absent]
        for (CourseClass courseClass : classes.findWhere(c -> c.isEnrolled(userId))) {
            counts.put(courseClass.getId(), new int[3]);
        }
        for (AttendanceRecord record : records.findWhere(r -> userId.equals(r.getUserId()))) {
            int[] count = counts.get(record.getClassId());
            if (count == null) {
                count = new int[3];
                counts.put(record.getClassId(), count);
            }
            count[record.getStatus().ordinal()]++;
        }

        DynamicArray<SubjectStat> stats = new DynamicArray<>();
        for (Long classId : counts.keys()) {
            CourseClass courseClass = classes.findById(classId);
            int[] count = counts.get(classId);
            int total = count[0] + count[1] + count[2];
            stats.add(new SubjectStat(
                    courseClass == null ? "-" : courseClass.getCode(),
                    courseClass == null ? "(deleted class)" : courseClass.getName(),
                    count[0], count[1], count[2], total,
                    total == 0 ? 0 : Math.round((count[0] + count[1]) * 100f / total)));
        }
        return Sorting.mergeSort(stats, Comparator.comparing(SubjectStat::code, String.CASE_INSENSITIVE_ORDER));
    }

    // ------------------------------------------------------------ admin, CSV

    /** Master log filter. Any argument may be null, meaning "all". */
    public DynamicArray<AttendanceRecord> filter(LocalDate date, Long classId, Role role) {
        return newestFirst(records.findWhere(r -> {
            if (date != null && !date.equals(r.getCheckInTime().toLocalDate())) return false;
            if (classId != null && !classId.equals(r.getClassId())) return false;
            if (role != null) {
                User user = users.findById(r.getUserId());
                return user != null && user.getRole() == role;
            }
            return true;
        }));
    }

    public DynamicArray<AttendanceRecord> recordsOfSession(Long sessionId) {
        return newestFirst(records.findWhere(r -> sessionId.equals(r.getSessionId())));
    }

    public DynamicArray<AttendanceRecord> recordsOfClass(Long classId) {
        return newestFirst(records.findWhere(r -> classId.equals(r.getClassId())));
    }

    public synchronized void edit(User admin, Long recordId, AttendanceStatus status, String note) {
        AttendanceRecord record = records.findById(recordId);
        if (record == null) throw new IllegalArgumentException("Attendance record not found");
        if (status == null) throw new IllegalArgumentException("Select a status");
        AttendanceStatus before = record.getStatus();
        record.setStatus(status);
        record.setNote(clean(note).isEmpty() ? "Edited by admin" : clean(note));
        records.save(record);
        audit.log(admin, "ATTENDANCE_EDIT", "Record " + recordId + ": " + before + " -> " + status);
    }

    public int recordCount() { return records.count(); }

    public String toCsv(DynamicArray<AttendanceRecord> rows) {
        StringBuilder csv = new StringBuilder("﻿"); // lets Excel detect UTF-8
        csv.append("Date,Time,Subject,Student,ID Number,Status,Method,Note\r\n");
        for (AttendanceRecord r : rows) {
            User student = users.findById(r.getUserId());
            CourseClass courseClass = classes.findById(r.getClassId());
            csv.append(r.getCheckInTime().format(CSV_DATE)).append(',')
                    .append(r.getCheckInTime().format(CSV_TIME)).append(',')
                    .append(cell(courseClass == null ? "" : courseClass.getLabel())).append(',')
                    .append(cell(student == null ? "(deleted user)" : student.getFullName())).append(',')
                    .append(cell(student == null ? "" : student.getIdNumber())).append(',')
                    .append(r.getStatus()).append(',')
                    .append(cell(r.getEntryMethod())).append(',')
                    .append(cell(r.getNote())).append("\r\n");
        }
        return csv.toString();
    }

    /** Quotes a CSV cell and defuses values a spreadsheet would run as a formula. */
    private static String cell(String value) {
        if (value == null) return "";
        if (!value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0) value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private AttendanceRecord newRecord(ClassSession session, Long userId, AttendanceStatus status,
                                       LocalDateTime time, String method) {
        AttendanceRecord record = new AttendanceRecord();
        record.setSessionId(session.getId());
        record.setClassId(session.getClassId());
        record.setUserId(userId);
        record.setCheckInTime(time);
        record.setStatus(status);
        record.setEntryMethod(method);
        return record;
    }

    private static DynamicArray<AttendanceRecord> newestFirst(DynamicArray<AttendanceRecord> rows) {
        return Sorting.mergeSort(rows, Comparator.comparing(AttendanceRecord::getCheckInTime).reversed());
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
