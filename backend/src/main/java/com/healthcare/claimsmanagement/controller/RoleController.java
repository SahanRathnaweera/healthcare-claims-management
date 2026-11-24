package com.healthcare.claimsmanagement.controller;

import com.healthcare.claimsmanagement.dto.RoleResponse;
import com.healthcare.claimsmanagement.service.RoleService;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMINISTRATOR')")
@Validated
public class RoleController {
    private final RoleService roles;

    public RoleController(RoleService roles) {
        this.roles = roles;
    }

    @GetMapping
    public List<RoleResponse> list() {
        return roles.list();
    }

    @GetMapping("/{id}")
    public RoleResponse get(@PathVariable @Positive(message = "Role id must be positive") Integer id) {
        return roles.get(id);
    }
}
