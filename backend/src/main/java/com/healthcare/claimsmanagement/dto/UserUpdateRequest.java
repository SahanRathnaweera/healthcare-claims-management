package com.healthcare.claimsmanagement.dto;

import jakarta.validation.constraints.*;

public record UserUpdateRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must not exceed 100 characters")
        String fullName,
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        String email,
        @NotNull(message = "Role is required")
        @Positive(message = "Role id must be positive")
        Integer roleId
) {
    public UserUpdateRequest {
        fullName = fullName == null ? null : fullName.trim();
        email = email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
