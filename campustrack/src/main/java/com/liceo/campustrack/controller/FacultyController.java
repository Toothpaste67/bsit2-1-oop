package com.liceo.campustrack.controller;

import com.liceo.campustrack.datastructure.DynamicArray;
import com.liceo.campustrack.dto.ApiResponse;
import com.liceo.campustrack.model.AttendanceStatus;
import com.liceo.campustrack.model.ClassSession;
import com.liceo.campustrack.model.CourseClass;
import com.liceo.campustrack.model.User;
import com.liceo.campustrack.security.CustomUserDetails;
import com.liceo.campustrack.service.AttendanceService;
import com.liceo.campustrack.service.AttendanceService.AttendanceRow;
import com.liceo.campustrack.service.QrCodeService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/faculty")
public class FacultyController {

    private final AttendanceService attendanceService;
    private final QrCodeService qrCodeService;

    public FacultyController(AttendanceService attendanceService,
                             QrCodeService qrCodeService) {
        this.attendanceService = attendanceService;
        this.qrCodeService = qrCodeService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails me, Model model) {
        User faculty = me.getUser();
        model.addAttribute("user", faculty);
        model.addAttribute("classes", attendanceService.classesOf(faculty));
        model.addAttribute("activeSessions", attendanceService.activeSessionsOf(faculty));
        model.addAttribute("recentSessions", attendanceService.recentSessionsOf(faculty, 10));
        model.addAttribute("attendance", attendanceService);
        return "faculty/faculty-dashboard";
    }

    // ---- classes ----

    @PostMapping("/classes")
    public String createClass(@AuthenticationPrincipal CustomUserDetails me,
                              @RequestParam String code,
                              @RequestParam String name,
                              @RequestParam(required = false) String schedule,
                              RedirectAttributes redirect) {
        try {
            CourseClass created = attendanceService.createClass(me.getUser(), code, name, schedule);
            redirect.addFlashAttribute("success", "Class " + created.getLabel() + " created.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/faculty/dashboard";
    }

    @PostMapping("/classes/{classId}/update")
    public String updateClass(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long classId,
                              @RequestParam String code,
                              @RequestParam String name,
                              @RequestParam(required = false) String schedule,
                              RedirectAttributes redirect) {
        try {
            attendanceService.updateClass(me.getUser(), classId, code, name, schedule);
            redirect.addFlashAttribute("success", "Class updated.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/faculty/dashboard";
    }

    @PostMapping("/classes/{classId}/delete")
    public String deleteClass(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long classId,
                              RedirectAttributes redirect) {
        try {
            attendanceService.deleteClass(me.getUser(), classId);
            redirect.addFlashAttribute("success", "Class deleted, together with its sessions and attendance records.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/faculty/dashboard";
    }

    @PostMapping("/classes/{classId}/enroll")
    public String enroll(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long classId,
                         @RequestParam String idNumbers, RedirectAttributes redirect) {
        try {
            redirect.addFlashAttribute("success", attendanceService.enroll(me.getUser(), classId, idNumbers));
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/faculty/dashboard";
    }

    @PostMapping("/classes/{classId}/unenroll")
    public String unenroll(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long classId,
                           @RequestParam Long studentId, RedirectAttributes redirect) {
        try {
            attendanceService.unenroll(me.getUser(), classId, studentId);
            redirect.addFlashAttribute("success", "Student removed from the class.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/faculty/dashboard";
    }

    @GetMapping("/classes/{classId}/export")
    public ResponseEntity<byte[]> exportClass(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long classId) {
        CourseClass courseClass = attendanceService.ownedClass(me.getUser(), classId);
        return csv(attendanceService.toCsv(attendanceService.recordsOfClass(classId)),
                "attendance-" + courseClass.getCode().replaceAll("[^A-Za-z0-9]+", "-") + ".csv");
    }

    // ---- sessions ----

    @PostMapping("/sessions/start")
    public String startSession(@AuthenticationPrincipal CustomUserDetails me,
                               @RequestParam(required = false) Long classId,
                               RedirectAttributes redirect) {
        try {
            ClassSession started = attendanceService.startSession(me.getUser(), classId);
            return "redirect:/faculty/sessions/" + started.getId();
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/faculty/dashboard";
        }
    }

    /** Presenter page: big rotating QR on the left, live attendance feed on the right. */
    @GetMapping("/sessions/{sessionId}")
    public String sessionView(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long sessionId, Model model) {
        ClassSession classSession = attendanceService.ownedSession(me.getUser(), sessionId);
        model.addAttribute("user", me.getUser());
        model.addAttribute("cs", classSession);
        model.addAttribute("rotateSeconds", QrCodeService.ROTATE_SECONDS);
        return "faculty/session-qr";
    }

    /**
     * Polled every few seconds by the presenter page. Returns the live counters and feed,
     * plus a fresh QR image whenever the 20-second window has moved on.
     */
    @GetMapping("/sessions/{sessionId}/live")
    @ResponseBody
    public Map<String, Object> live(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long sessionId,
                                    @RequestParam(required = false) Long window) {
        ClassSession classSession = attendanceService.ownedSession(me.getUser(), sessionId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("active", classSession.isActive());

        if (classSession.isActive()) {
            long current = qrCodeService.currentWindow();
            body.put("window", current);
            body.put("secondsLeft", qrCodeService.secondsLeftInWindow());
            if (window == null || window != current) {
                String payload = qrCodeService.sessionPayload(classSession, current);
                body.put("qr", "data:image/png;base64," + qrCodeService.toPngBase64(payload, 520));
                body.put("manualCode", qrCodeService.manualCode(classSession, current));
            }
        }

        DynamicArray<AttendanceRow> rows = attendanceService.liveRows(classSession);
        body.put("present", rows.filter(r -> r.status().equals("PRESENT")).size());
        body.put("late", rows.filter(r -> r.status().equals("LATE")).size());
        body.put("absent", rows.filter(r -> r.status().equals("ABSENT")).size());
        body.put("waiting", rows.filter(r -> r.status().equals("WAITING")).size());
        body.put("total", rows.size());
        body.put("rows", rows.toList());
        return body;
    }

    @PostMapping("/sessions/{sessionId}/override")
    @ResponseBody
    public ApiResponse override(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long sessionId,
                                @RequestParam Long studentId, @RequestParam AttendanceStatus status) {
        try {
            attendanceService.override(me.getUser(), sessionId, studentId, status);
            return ApiResponse.ok("Marked " + status, null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/sessions/{sessionId}/end")
    public String endSession(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long sessionId,
                             RedirectAttributes redirect) {
        attendanceService.endSession(me.getUser(), sessionId);
        redirect.addFlashAttribute("success", "Session ended. Students who did not check in were marked absent.");
        return "redirect:/faculty/sessions/" + sessionId;
    }

    @GetMapping("/sessions/{sessionId}/export")
    public ResponseEntity<byte[]> exportSession(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long sessionId) {
        attendanceService.ownedSession(me.getUser(), sessionId);
        return csv(attendanceService.toCsv(attendanceService.recordsOfSession(sessionId)),
                "attendance-session-" + sessionId + ".csv");
    }

    static ResponseEntity<byte[]> csv(String content, String fileName) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(content.getBytes(StandardCharsets.UTF_8));
    }
}
