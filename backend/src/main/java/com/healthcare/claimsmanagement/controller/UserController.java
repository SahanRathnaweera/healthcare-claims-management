package com.healthcare.claimsmanagement.controller;

import com.healthcare.claimsmanagement.dto.*;
import com.healthcare.claimsmanagement.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMINISTRATOR')")
public class UserController {
    private final UserAdminService users;

    public UserController(UserAdminService users) {
        this.users = users;
    }

    @GetMapping
    public Page<UserResponse> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer roleId,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20, sort = "userId", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return users.search(search, roleId, isActive, pageable);
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Integer id) {
        return users.get(id);
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        UserResponse created = users.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.userId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Integer id,
                               @Valid @RequestBody UserUpdateRequest request,
                               Authentication authentication) {
        return users.update(id, request, authentication);
    }

    @PatchMapping("/{id}/status")
    @ResponseStatus(HttpStatus.OK)
    public UserResponse setStatus(@PathVariable Integer id,
                                  @Valid @RequestBody UserStatusRequest request,
                                  Authentication authentication) {
        return users.setStatus(id, request, authentication);
    }

    @PatchMapping("/{id}/role")
    public UserResponse setRole(@PathVariable Integer id,
                                @Valid @RequestBody UserRoleRequest request,
                                Authentication authentication) {
        return users.setRole(id, request, authentication);
    }

    @PatchMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable Integer id,
                              @Valid @RequestBody PasswordResetRequest request) {
        users.resetPassword(id, request);
    }
}
