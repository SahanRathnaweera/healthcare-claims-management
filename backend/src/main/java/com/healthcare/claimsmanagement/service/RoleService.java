package com.healthcare.claimsmanagement.service;

import com.healthcare.claimsmanagement.dto.RoleResponse;

import java.util.List;

public interface RoleService {
    List<RoleResponse> list();
    RoleResponse get(Integer id);
}
