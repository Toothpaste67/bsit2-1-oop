package com.liceo.campustrack.model;

import java.time.LocalDateTime;

public class AuditLog {
    private final LocalDateTime timestamp;
    private final String actor;
    private final String action;
    private final String details;

    public AuditLog(LocalDateTime timestamp, String actor, String action, String details) {
        this.timestamp = timestamp;
        this.actor = actor;
        this.action = action;
        this.details = details;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public String getActor() { return actor; }
    public String getAction() { return action; }
    public String getDetails() { return details; }
}
