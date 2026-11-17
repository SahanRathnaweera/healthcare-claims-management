package com.healthcare.claimsmanagement.dto;

import jakarta.validation.constraints.NotNull;

public record ClaimStatusUpdateRequest(@NotNull ClaimStatus newStatus) {}
