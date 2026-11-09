package com.healthcare.claimsmanagement.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ClaimCreateRequest(
    @NotBlank @Size(max = 30) String claimReference,
    @NotBlank @Size(max = 50) String patientRef,
    @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal claimAmount,
    @PastOrPresent LocalDate submissionDate
) {}
