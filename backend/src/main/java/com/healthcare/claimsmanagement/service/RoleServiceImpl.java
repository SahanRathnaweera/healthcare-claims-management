package com.healthcare.claimsmanagement.service;

import com.healthcare.claimsmanagement.dto.RoleResponse;
import com.healthcare.claimsmanagement.exception.ResourceNotFoundException;
import com.healthcare.claimsmanagement.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {
    private final RoleRepository roles;

    public RoleServiceImpl(RoleRepository roles) {
        this.roles = roles;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> list() {
        return roles.findAll().stream().map(RoleResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse get(Integer id) {
        return roles.findById(id)
                .map(RoleResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
    }
}
