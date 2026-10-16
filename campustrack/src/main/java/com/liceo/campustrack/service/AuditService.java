package com.liceo.campustrack.service;

import com.liceo.campustrack.datastructure.DynamicArray;
import com.liceo.campustrack.model.AuditLog;
import com.liceo.campustrack.model.User;
import com.liceo.campustrack.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(User actor, String action, String details) {
        log(actor == null ? "system" : actor.getEmail(), action, details);
    }

    public void log(String actor, String action, String details) {
        auditLogRepository.append(new AuditLog(LocalDateTime.now(), actor, action, details));
    }

    public DynamicArray<AuditLog> newest(int max) {
        return auditLogRepository.newest(max);
    }
}
