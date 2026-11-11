package com.healthcare.claimsmanagement.service;

import com.healthcare.claimsmanagement.common.RoleName;
import com.healthcare.claimsmanagement.security.AccessPolicy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class UserManagementAuthorization {
    public void requireAdministrator(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(authority -> AccessPolicy.ADMIN.equals(authority.getAuthority()))) {
            throw new AccessDeniedException("Administrator access required");
        }
    }

    public void requireCannotChangeOwnRole(Authentication authentication, Integer targetUserId) {
        Integer currentUserId = currentUserId(authentication);
        if (currentUserId.equals(targetUserId)) {
            throw new AccessDeniedException("Users cannot change their own role");
        }
    }

    public void requireCannotDeactivateSelf(Authentication authentication, Integer targetUserId,
                                             boolean active) {
        if (!active && currentUserId(authentication).equals(targetUserId)) {
            throw new AccessDeniedException("Users cannot deactivate themselves");
        }
    }

    public RoleName requireKnownRole(String databaseRoleName) {
        try {
            return RoleName.fromDatabaseName(databaseRoleName);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown role value", ex);
        }
    }

    private Integer currentUserId(Authentication authentication) {
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (!(principal instanceof com.healthcare.claimsmanagement.security.AuthenticatedUser user)) {
            throw new AccessDeniedException("Authenticated user is unavailable");
        }
        return user.getUserId();
    }
}
