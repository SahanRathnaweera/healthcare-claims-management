package com.healthcare.claimsmanagement.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

public final class AuthenticatedUser extends User {
    private final Integer userId;

    public AuthenticatedUser(Integer userId, String email, String password, boolean enabled,
                             Collection<? extends GrantedAuthority> authorities) {
        super(email, password, enabled, true, true, true, authorities);
        this.userId = userId;
    }

    public Integer getUserId() {
        return userId;
    }
}
