package com.healthcare.claimsmanagement.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {
    public AuthenticatedUser requireUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new IllegalStateException("Authenticated user is not available");
        }
        return user;
    }

    public Integer requireUserId(Authentication authentication) {
        return requireUser(authentication).getUserId();
    }

    public String requireRole(Authentication authentication) {
        return requireUser(authentication).getAuthorities().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Authenticated role is not available"))
                .getAuthority();
    }
}
