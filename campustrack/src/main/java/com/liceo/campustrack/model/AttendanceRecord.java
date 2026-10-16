package com.liceo.campustrack.model;

import java.time.LocalDateTime;

public class AttendanceRecord {
    private Long id;
    private Long sessionId;
    private Long classId;
    private Long userId;
    private LocalDateTime checkInTime;
    private AttendanceStatus status;
    private String entryMethod;
    private String note;

    public AttendanceRecord() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDateTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalDateTime checkInTime) { this.checkInTime = checkInTime; }
    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus status) { this.status = status; }
    public String getEntryMethod() { return entryMethod; }
    public void setEntryMethod(String entryMethod) { this.entryMethod = entryMethod; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
