package com.healthcare.claimsmanagement.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Size(min = 2, max = 100) String fullName,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(min = 8, max = 64)
        @Pattern(regexp = ".*[A-Z].*", message = "must contain an uppercase letter")
        @Pattern(regexp = ".*[a-z].*", message = "must contain a lowercase letter")
        @Pattern(regexp = ".*\\d.*", message = "must contain a digit")
        @Pattern(regexp = ".*[^a-zA-Z0-9].*", message = "must contain a special character")
        String password,
        @NotBlank String confirmPassword) {}
