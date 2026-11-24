package com.healthcare.claimsmanagement.dto;

import jakarta.validation.constraints.*;

public record PasswordResetRequest(
        @NotBlank @Size(min = 8, max = 64)
        @Pattern(regexp = ".*[A-Za-z].*", message = "must contain a letter")
        @Pattern(regexp = ".*\\d.*", message = "must contain a digit")
        String password
) {}
