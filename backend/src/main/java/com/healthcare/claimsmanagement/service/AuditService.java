package com.healthcare.claimsmanagement.service;

import com.healthcare.claimsmanagement.entity.*;
import com.healthcare.claimsmanagement.repository.AuditLogRepository;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository repository;
    private final boolean authEventsEnabled;

    public AuditService(AuditLogRepository repository,
                        @Value("${app.audit.auth-events-enabled:false}") boolean authEventsEnabled) {
        this.repository = repository;
        this.authEventsEnabled = authEventsEnabled;
    }

    public void register(Integer userId) { write(null, userId, "INSERT", "Self-registration (pending approval)", true); }
    public void auth(Integer userId, String action) { write(userId, userId, action, null, authEventsEnabled); }

    private void write(Integer userId, Integer recordId, String action, String details, boolean enabled) {
        if (!enabled) return;
        try {
            repository.save(new AuditLog(userId, "users", recordId, action, details));
        } catch (RuntimeException ex) {
            log.error("Unable to write audit event action={} recordId={}", action, recordId, ex);
        }
    }
}
