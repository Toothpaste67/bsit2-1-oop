package com.liceo.campustrack.model;

import java.time.LocalDateTime;

/** One meeting of a class. Its secret signs the rotating attendance QR codes. */
public class ClassSession {
    private Long id;
    private Long classId;
    private Long facultyId;
    private String secret;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    public ClassSession() {}

    public boolean isActive() { return endedAt == null; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }
    public Long getFacultyId() { return facultyId; }
    public void setFacultyId(Long facultyId) { this.facultyId = facultyId; }
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
}
