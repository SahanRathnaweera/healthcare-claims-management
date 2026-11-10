package com.healthcare.claimsmanagement.dto;

public record LoginResponse(String token, String tokenType, long expiresIn, UserProfile user) {}
