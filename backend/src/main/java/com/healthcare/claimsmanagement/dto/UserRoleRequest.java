package com.healthcare.claimsmanagement.dto;

import jakarta.validation.constraints.NotNull;

public record UserRoleRequest(@NotNull Integer roleId) {}
