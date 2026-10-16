package com.liceo.campustrack.repository;

import com.liceo.campustrack.datastructure.DynamicArray;
import com.liceo.campustrack.datastructure.HashTable;
import com.liceo.campustrack.model.AttendanceRecord;
import com.liceo.campustrack.model.AttendanceStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class AttendanceRepository extends TextFileRepository<AttendanceRecord> {

    // "sessionId:userId" -> record. Enforces one check-in per student per session in O(1).
    private HashTable<String, AttendanceRecord> bySessionAndUser = new HashTable<>();

    public AttendanceRepository(@Value("${campustrack.data-dir}") String dataDir) {
        super(dataDir, "attendance.txt", "id|sessionId|classId|userId|checkInTime|status|entryMethod|note");
    }

    public synchronized AttendanceRecord findBySessionAndUser(Long sessionId, Long userId) {
        return bySessionAndUser.get(sessionId + ":" + userId);
    }

    @Override
    protected void reindex(DynamicArray<AttendanceRecord> all) {
        HashTable<String, AttendanceRecord> index = new HashTable<>();
        for (AttendanceRecord record : all) {
            index.put(record.getSessionId() + ":" + record.getUserId(), record);
        }
        bySessionAndUser = index;
    }

    @Override protected Long getId(AttendanceRecord r) { return r.getId(); }
    @Override protected void setId(AttendanceRecord r, Long id) { r.setId(id); }

    @Override
    protected String[] toFields(AttendanceRecord r) {
        return new String[] {
                text(r.getId()), text(r.getSessionId()), text(r.getClassId()), text(r.getUserId()),
                text(r.getCheckInTime()), text(r.getStatus()), text(r.getEntryMethod()), text(r.getNote())
        };
    }

    @Override
    protected AttendanceRecord fromFields(String[] f) {
        AttendanceRecord r = new AttendanceRecord();
        r.setId(number(f, 0));
        r.setSessionId(number(f, 1));
        r.setClassId(number(f, 2));
        r.setUserId(number(f, 3));
        r.setCheckInTime(dateTime(f, 4));
        r.setStatus(constant(AttendanceStatus.class, f, 5));
        r.setEntryMethod(field(f, 6));
        r.setNote(field(f, 7));
        return r;
    }
}
