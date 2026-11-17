package com.healthcare.claimsmanagement.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "claim_status_history")
public class ClaimStatusHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Integer historyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @Column(name = "previous_status", length = 20)
    private String previousStatus;

    @Column(name = "new_status", nullable = false, length = 20)
    private String newStatus;

    @Column(name = "changed_by")
    private Integer changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    public Integer getHistoryId() { return historyId; }
    public Claim getClaim() { return claim; }
    public String getPreviousStatus() { return previousStatus; }
    public String getNewStatus() { return newStatus; }
    public Integer getChangedBy() { return changedBy; }
    public LocalDateTime getChangedAt() { return changedAt; }
}
