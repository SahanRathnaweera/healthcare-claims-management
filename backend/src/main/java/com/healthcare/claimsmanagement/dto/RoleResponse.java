package com.healthcare.claimsmanagement.dto;

import com.healthcare.claimsmanagement.entity.Role;

public record RoleResponse(Integer roleId, String roleName) {
    public static RoleResponse from(Role role) {
        return new RoleResponse(role.getRoleId(), role.getRoleName());
    }
}
