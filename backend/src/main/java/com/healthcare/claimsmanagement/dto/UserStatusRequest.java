package com.healthcare.claimsmanagement.dto;

import jakarta.validation.constraints.NotNull;

public record UserStatusRequest(
        @NotNull(message = "Active status is required")
        Boolean isActive
) {}
