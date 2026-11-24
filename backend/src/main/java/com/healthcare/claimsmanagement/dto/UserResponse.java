package com.healthcare.claimsmanagement.dto;

import com.healthcare.claimsmanagement.entity.User;

import java.time.LocalDateTime;

public record UserResponse(
        Integer userId,
        String fullName,
        String email,
        Integer roleId,
        String roleName,
        Boolean isActive,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().getRoleId(),
                user.getRole().getRoleName(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
