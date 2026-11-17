package com.healthcare.claimsmanagement.dto;

import com.healthcare.claimsmanagement.entity.ClaimStatusHistory;
import java.time.LocalDateTime;

public record ClaimHistoryResponse(
    Integer historyId, Integer claimId, String previousStatus,
    String newStatus, Integer changedBy, LocalDateTime changedAt
) {
    public static ClaimHistoryResponse from(ClaimStatusHistory h) {
        return new ClaimHistoryResponse(h.getHistoryId(), h.getClaim().getClaimId(),
            h.getPreviousStatus(), h.getNewStatus(), h.getChangedBy(), h.getChangedAt());
    }
}
