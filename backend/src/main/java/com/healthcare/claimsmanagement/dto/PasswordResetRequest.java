package com.healthcare.claimsmanagement.dto;

import jakarta.validation.constraints.*;

public record PasswordResetRequest(
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        @Pattern(regexp = ".*[A-Za-z].*", message = "Password must contain at least one letter")
        @Pattern(regexp = ".*\\d.*", message = "Password must contain at least one digit")
        String password
) {}
