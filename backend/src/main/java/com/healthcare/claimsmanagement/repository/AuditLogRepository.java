package com.healthcare.claimsmanagement.repository;

import com.healthcare.claimsmanagement.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {
}
