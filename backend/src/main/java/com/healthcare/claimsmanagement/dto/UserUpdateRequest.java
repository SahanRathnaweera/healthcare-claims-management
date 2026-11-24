package com.healthcare.claimsmanagement.dto;

import jakarta.validation.constraints.*;

public record UserUpdateRequest(
        @NotBlank @Size(min = 2, max = 100) String fullName,
        @NotBlank @Email @Size(max = 100) String email,
        @NotNull Integer roleId
) {}
