package com.healthcare.claimsmanagement.security;

import com.healthcare.claimsmanagement.entity.User;
import com.healthcare.claimsmanagement.repository.UserRepository;
import com.healthcare.claimsmanagement.common.RoleName;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository users;

    public CustomUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = users.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .disabled(!user.isActive())
                .authorities(RoleName.fromDatabaseName(user.getRole().getRoleName()).authority())
                .build();
    }
}
