package com.healthcare.claimsmanagement.security;

import com.healthcare.claimsmanagement.entity.User;
import com.healthcare.claimsmanagement.repository.UserRepository;
import com.healthcare.claimsmanagement.common.RoleName;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
        return new AuthenticatedUser(user.getUserId(), user.getEmail(), user.getPasswordHash(),
                user.isActive(),
                java.util.List.of(new SimpleGrantedAuthority(
                        RoleName.fromDatabaseName(user.getRole().getRoleName()).authority())));
    }
}
