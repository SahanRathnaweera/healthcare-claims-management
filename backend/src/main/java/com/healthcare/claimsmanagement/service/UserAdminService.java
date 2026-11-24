package com.healthcare.claimsmanagement.service;

import com.healthcare.claimsmanagement.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

public interface UserAdminService {
    Page<UserResponse> search(String search, Integer roleId, Boolean isActive, Pageable pageable);
    UserResponse get(Integer id);
    UserResponse create(UserCreateRequest request);
    UserResponse update(Integer id, UserUpdateRequest request, Authentication authentication);
    UserResponse setStatus(Integer id, UserStatusRequest request, Authentication authentication);
    UserResponse setRole(Integer id, UserRoleRequest request, Authentication authentication);
    void resetPassword(Integer id, PasswordResetRequest request);
}
