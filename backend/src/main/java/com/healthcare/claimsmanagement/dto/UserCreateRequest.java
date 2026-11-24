package com.healthcare.claimsmanagement.dto;

import jakarta.validation.constraints.*;

public record UserCreateRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must not exceed 100 characters")
        String fullName,
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        String email,
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        @Pattern(regexp = ".*[A-Za-z].*", message = "Password must contain at least one letter")
        @Pattern(regexp = ".*\\d.*", message = "Password must contain at least one digit")
        String password,
        @NotNull(message = "Role is required")
        @Positive(message = "Role id must be positive")
        Integer roleId,
        @NotNull(message = "Active status is required")
        Boolean isActive
) {
    public UserCreateRequest {
        fullName = fullName == null ? null : fullName.trim();
        email = email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
