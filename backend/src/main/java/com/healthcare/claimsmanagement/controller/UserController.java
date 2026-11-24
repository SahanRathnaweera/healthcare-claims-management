package com.healthcare.claimsmanagement.controller;

import com.healthcare.claimsmanagement.dto.*;
import com.healthcare.claimsmanagement.service.UserAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMINISTRATOR')")
@Validated
public class UserController {
    private static final Set<String> SORTABLE_FIELDS =
            Set.of("fullName", "email", "createdAt", "isActive");
    private final UserAdminService users;

    public UserController(UserAdminService users) {
        this.users = users;
    }

    @GetMapping
    public Page<UserResponse> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @Positive(message = "Role id must be positive") Integer roleId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page must not be negative") @Max(value = 10000, message = "Page is too large") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "Size must be at least 1") @Max(value = 100, message = "Size must not exceed 100") int size,
            @RequestParam(required = false) String sort,
            @PageableDefault(size = 20, sort = "userId", direction = Sort.Direction.ASC)
            Pageable pageable) {
        Sort sortOptions = sort == null || sort.isBlank()
                ? Sort.by(Sort.Direction.ASC, "createdAt")
                : parseSort(sort);
        return users.search(search, roleId, isActive, PageRequest.of(page, size, sortOptions));
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable @Positive(message = "User id must be positive") Integer id) {
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
    public UserResponse update(@PathVariable @Positive(message = "User id must be positive") Integer id,
                               @Valid @RequestBody UserUpdateRequest request,
                               Authentication authentication) {
        return users.update(id, request, authentication);
    }

    @PatchMapping("/{id}/status")
    @ResponseStatus(HttpStatus.OK)
    public UserResponse setStatus(@PathVariable @Positive(message = "User id must be positive") Integer id,
                                  @Valid @RequestBody UserStatusRequest request,
                                  Authentication authentication) {
        return users.setStatus(id, request, authentication);
    }

    @PatchMapping("/{id}/role")
    public UserResponse setRole(@PathVariable @Positive(message = "User id must be positive") Integer id,
                                @Valid @RequestBody UserRoleRequest request,
                                Authentication authentication) {
        return users.setRole(id, request, authentication);
    }

    @PatchMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable @Positive(message = "User id must be positive") Integer id,
                              @Valid @RequestBody PasswordResetRequest request) {
        users.resetPassword(id, request);
    }

    private Sort parseSort(String value) {
        String[] parts = value.split(",");
        if (parts.length > 2) {
            throw new com.healthcare.claimsmanagement.exception.BadRequestException("Invalid sort parameter");
        }
        String field = parts[0].trim();
        if (!SORTABLE_FIELDS.contains(field)) {
            throw new com.healthcare.claimsmanagement.exception.BadRequestException("Invalid sort field");
        }
        String entityField = "isActive".equals(field) ? "active" : field;
        if (parts.length == 1) {
            return Sort.by(Sort.Direction.ASC, entityField);
        }
        String direction = parts[1].trim();
        if ("asc".equalsIgnoreCase(direction)) {
            return Sort.by(Sort.Direction.ASC, entityField);
        }
        if ("desc".equalsIgnoreCase(direction)) {
            return Sort.by(Sort.Direction.DESC, entityField);
        }
        throw new com.healthcare.claimsmanagement.exception.BadRequestException("Invalid sort direction");
    }
}
