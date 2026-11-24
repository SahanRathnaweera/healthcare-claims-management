package com.healthcare.claimsmanagement.service;

import com.healthcare.claimsmanagement.common.RoleName;
import com.healthcare.claimsmanagement.dto.*;
import com.healthcare.claimsmanagement.entity.Role;
import com.healthcare.claimsmanagement.entity.User;
import com.healthcare.claimsmanagement.exception.BadRequestException;
import com.healthcare.claimsmanagement.exception.DuplicateResourceException;
import com.healthcare.claimsmanagement.exception.ResourceNotFoundException;
import com.healthcare.claimsmanagement.repository.RoleRepository;
import com.healthcare.claimsmanagement.repository.UserRepository;
import com.healthcare.claimsmanagement.security.AuthenticatedUser;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Locale;

@Service
public class UserAdminServiceImpl implements UserAdminService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;

    public UserAdminServiceImpl(UserRepository users, RoleRepository roles,
                                PasswordEncoder passwordEncoder) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> search(String search, Integer roleId, Boolean isActive, Pageable pageable) {
        Specification<User> specification = (root, query, criteriaBuilder) -> {
            java.util.List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern)
                ));
            }
            if (roleId != null) {
                predicates.add(criteriaBuilder.equal(root.get("role").get("roleId"), roleId));
            }
            if (isActive != null) {
                predicates.add(criteriaBuilder.equal(root.get("active"), isActive));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
        return users.findAll(specification, pageable).map(UserResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse get(Integer id) {
        return UserResponse.from(findUser(id));
    }

    @Override
    @Transactional
    public UserResponse create(UserCreateRequest request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Email is already registered");
        }
        Role role = findRole(request.roleId());
        User user = new User(
                request.fullName().trim(),
                email,
                passwordEncoder.encode(request.password()),
                role,
                request.isActive()
        );
        return UserResponse.from(users.saveAndFlush(user));
    }

    @Override
    @Transactional
    public UserResponse update(Integer id, UserUpdateRequest request,
                               Authentication authentication) {
        User user = findUser(id);
        String email = normalizeEmail(request.email());
        if (!email.equalsIgnoreCase(user.getEmail())
                && users.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Email is already registered");
        }

        Role role = findRole(request.roleId());
        if (currentUserId(authentication).equals(user.getUserId())
                && isActiveAdministrator(user)
                && !role.getRoleName().equals(RoleName.SYSTEM_ADMINISTRATOR.databaseName())) {
            throw new BadRequestException("Users cannot remove their own administrator role");
        }
        if (isActiveAdministrator(user)
                && !role.getRoleName().equals(RoleName.SYSTEM_ADMINISTRATOR.databaseName())) {
            protectLastActiveAdministrator(user);
        }

        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setRole(role);
        return UserResponse.from(users.saveAndFlush(user));
    }

    @Override
    @Transactional
    public UserResponse setStatus(Integer id, UserStatusRequest request,
                                  Authentication authentication) {
        User user = findUser(id);
        if (!request.isActive() && currentUserId(authentication).equals(user.getUserId())) {
            throw new BadRequestException("Users cannot deactivate themselves");
        }
        if (!request.isActive() && isActiveAdministrator(user)) {
            protectLastActiveAdministrator(user);
        }
        user.setActive(request.isActive());
        return UserResponse.from(users.saveAndFlush(user));
    }

    @Override
    @Transactional
    public UserResponse setRole(Integer id, UserRoleRequest request,
                                Authentication authentication) {
        User user = findUser(id);
        if (currentUserId(authentication).equals(user.getUserId())
                && !findRole(request.roleId()).getRoleName()
                .equals(RoleName.SYSTEM_ADMINISTRATOR.databaseName())) {
            throw new BadRequestException("Users cannot remove their own administrator role");
        }
        Role role = findRole(request.roleId());
        if (isActiveAdministrator(user)
                && !role.getRoleName().equals(RoleName.SYSTEM_ADMINISTRATOR.databaseName())) {
            protectLastActiveAdministrator(user);
        }
        user.setRole(role);
        return UserResponse.from(users.saveAndFlush(user));
    }

    @Override
    @Transactional
    public void resetPassword(Integer id, PasswordResetRequest request) {
        User user = findUser(id);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        users.save(user);
    }

    private User findUser(Integer id) {
        return users.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("User not found"));
    }

    private Role findRole(Integer roleId) {
        return roles.findById(roleId).orElseThrow(
                () -> new ResourceNotFoundException("Role not found"));
    }

    private boolean isActiveAdministrator(User user) {
        return user.isActive()
                && RoleName.SYSTEM_ADMINISTRATOR.databaseName()
                .equals(user.getRole().getRoleName());
    }

    private void protectLastActiveAdministrator(User user) {
        long activeAdministrators = users.countByRoleRoleNameAndActiveTrue(
                RoleName.SYSTEM_ADMINISTRATOR.databaseName());
        if (activeAdministrators <= 1) {
            throw new BadRequestException("Cannot remove or deactivate the last active System Administrator");
        }
    }

    private Integer currentUserId(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new BadRequestException("Authenticated user is unavailable");
        }
        return principal.getUserId();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
