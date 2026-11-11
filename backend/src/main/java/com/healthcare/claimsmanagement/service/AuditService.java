package com.healthcare.claimsmanagement.service;

import com.healthcare.claimsmanagement.entity.*;
import com.healthcare.claimsmanagement.repository.AuditLogRepository;
import com.healthcare.claimsmanagement.repository.UserRepository;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository repository;
    private final UserRepository users;
    private final boolean authEventsEnabled;

    public AuditService(AuditLogRepository repository, UserRepository users,
                        @Value("${app.audit.auth-events-enabled:false}") boolean authEventsEnabled) {
        this.repository = repository;
        this.users = users;
        this.authEventsEnabled = authEventsEnabled;
    }

    public void register(Integer userId) { write(null, userId, "INSERT", "Self-registration (pending approval)", true); }
    public void auth(Integer userId, String action) { write(userId, userId, action, null, authEventsEnabled); }

    public void denied(String email, String method, String path) {
        try {
            Integer userId = users.findByEmail(email.toLowerCase(java.util.Locale.ROOT))
                    .map(entity -> entity.getUserId())
                    .orElse(null);
            log.warn("Denied access userId={} method={} path={}", userId, method, path);
        } catch (RuntimeException ex) {
            log.error("Unable to resolve user for denied access method={} path={}", method, path, ex);
        }
    }

    private void write(Integer userId, Integer recordId, String action, String details, boolean enabled) {
        if (!enabled) return;
        try {
            repository.save(new AuditLog(userId, "users", recordId, action, details));
        } catch (RuntimeException ex) {
            log.error("Unable to write audit event action={} recordId={}", action, recordId, ex);
        }
    }
}
