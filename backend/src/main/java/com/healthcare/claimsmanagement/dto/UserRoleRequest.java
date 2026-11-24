package com.healthcare.claimsmanagement.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UserRoleRequest(
        @NotNull(message = "Role is required")
        @Positive(message = "Role id must be positive")
        Integer roleId
) {}
