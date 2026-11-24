package com.healthcare.claimsmanagement.dto;

import jakarta.validation.constraints.*;

public record LoginRequest(@NotBlank @Email @Size(max = 100) String email,
                           @NotBlank String password,
                           Boolean rememberMe) {
    public LoginRequest {
        email = email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
