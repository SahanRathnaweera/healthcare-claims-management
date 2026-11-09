package com.healthcare.claimsmanagement.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "claims")
public class Claim {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "claim_id")
    private Integer claimId;

    @Column(name = "claim_reference", nullable = false, unique = true, length = 30)
    private String claimReference;

    @Column(name = "patient_ref", nullable = false, length = 50)
    private String patientRef;

    @Column(name = "claim_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal claimAmount;

    @Column(name = "submission_date", nullable = false)
    private LocalDate submissionDate;

    @Column(name = "current_status", nullable = false, length = 20)
    private String currentStatus;

    @Column(name = "created_by")
    private Integer createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        var now = LocalDateTime.now();
        if (submissionDate == null) submissionDate = LocalDate.now();
        if (currentStatus == null) currentStatus = "SUBMITTED";
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Integer getClaimId() { return claimId; }
    public String getClaimReference() { return claimReference; }
    public void setClaimReference(String value) { this.claimReference = value; }
    public String getPatientRef() { return patientRef; }
    public void setPatientRef(String value) { this.patientRef = value; }
    public BigDecimal getClaimAmount() { return claimAmount; }
    public void setClaimAmount(BigDecimal value) { this.claimAmount = value; }
    public LocalDate getSubmissionDate() { return submissionDate; }
    public void setSubmissionDate(LocalDate value) { this.submissionDate = value; }
    public String getCurrentStatus() { return currentStatus; }
    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer value) { this.createdBy = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
