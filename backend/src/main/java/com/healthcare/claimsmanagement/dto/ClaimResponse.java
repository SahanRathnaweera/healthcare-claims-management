package com.healthcare.claimsmanagement.dto;

import com.healthcare.claimsmanagement.entity.Claim;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ClaimResponse(
    Integer claimId, String claimReference, String patientRef,
    BigDecimal claimAmount, LocalDate submissionDate, String currentStatus,
    Integer createdBy, LocalDateTime createdAt, LocalDateTime updatedAt
) {
    public static ClaimResponse from(Claim c) {
        return new ClaimResponse(c.getClaimId(), c.getClaimReference(), c.getPatientRef(),
            c.getClaimAmount(), c.getSubmissionDate(), c.getCurrentStatus(),
            c.getCreatedBy(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
