package com.healthcare.claimsmanagement.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Integer auditId;
    @Column(name = "user_id")
    private Integer userId;
    @Column(name = "entity_name", nullable = false, length = 50)
    private String entityName;
    @Column(name = "record_id", nullable = false)
    private Integer recordId;
    @Column(nullable = false, length = 20)
    private String action;
    @Column(columnDefinition = "TEXT")
    private String details;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected AuditLog() {}

    public AuditLog(Integer userId, String entityName, Integer recordId, String action, String details) {
        this.userId = userId;
        this.entityName = entityName;
        this.recordId = recordId;
        this.action = action;
        this.details = details;
    }

    @PrePersist
    void setCreatedAt() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
