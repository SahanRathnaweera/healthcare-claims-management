package com.healthcare.claimsmanagement.dto;

import jakarta.validation.constraints.*;

public record LoginRequest(@NotBlank @Email @Size(max = 100) String email,
                           @NotBlank String password,
                           Boolean rememberMe) {}
