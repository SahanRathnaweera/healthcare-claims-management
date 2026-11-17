package com.healthcare.claimsmanagement.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

// Details only. The claim reference and status must not be changed through this endpoint.
public record ClaimUpdateRequest(
    @NotBlank @Size(max = 50) String patientRef,
    @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal claimAmount,
    @NotNull @PastOrPresent LocalDate submissionDate
) {}
