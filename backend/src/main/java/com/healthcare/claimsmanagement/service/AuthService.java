package com.healthcare.claimsmanagement.service;

import com.healthcare.claimsmanagement.common.RoleName;
import com.healthcare.claimsmanagement.dto.*;
import com.healthcare.claimsmanagement.entity.User;
import com.healthcare.claimsmanagement.exception.*;
import com.healthcare.claimsmanagement.repository.*;
import com.healthcare.claimsmanagement.security.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String INVALID_CREDENTIALS = "Invalid email or password";
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklist blacklist;
    private final AuditService audit;
    private final long expirationMs;
    private final long rememberExpirationMs;

    public AuthService(UserRepository users, RoleRepository roles, PasswordEncoder passwordEncoder,
                       JwtService jwtService, TokenBlacklist blacklist, AuditService audit,
                       @Value("${app.jwt.expiration-ms}") long expirationMs,
                       @Value("${app.jwt.remember-me-expiration-ms:604800000}") long rememberExpirationMs) {
        this.users = users; this.roles = roles; this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService; this.blacklist = blacklist; this.audit = audit;
        this.expirationMs = expirationMs; this.rememberExpirationMs = rememberExpirationMs;
    }

    /** Authenticates an active user and issues a signed JWT. */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = normalize(request.email());
        User user = users.findByEmail(email).orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Failed login for unknown or invalid credentials");
            if (user != null) audit.auth(user.getUserId(), "LOGIN_FAILED");
            throw new AuthenticationException(INVALID_CREDENTIALS);
        }
        if (!user.isActive()) throw new InactiveAccountException("Account is inactive or pending approval");
        long expiry = Boolean.TRUE.equals(request.rememberMe()) ? rememberExpirationMs : expirationMs;
        String token = jwtService.generateToken(user, expiry);
        audit.auth(user.getUserId(), "LOGIN");
        return new LoginResponse(token, "Bearer", expiry / 1000,
                profile(user));
    }

    /** Registers an inactive manager/analyst account after validating uniqueness and credentials. */
    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new PasswordMismatchException("Password and confirmPassword must match");
        }
        String email = normalize(request.email());
        if (users.existsByEmail(email)) throw new DuplicateEmailException("Email is already registered");
        var role = roles.findByRoleName(RoleName.MANAGER_ANALYST.databaseName())
                .orElseThrow(() -> new IllegalStateException("Default registration role is not configured"));
        User user = new User(request.fullName().trim(), email,
                passwordEncoder.encode(request.password()), role, false);
        User saved = users.save(user);
        audit.register(saved.getUserId());
        return new RegistrationResponse(
                "Registration successful. Your account is pending administrator approval.",
                saved.getUserId(), saved.getEmail());
    }

    /** Returns the profile associated with the authenticated principal. */
    @Transactional(readOnly = true)
    public UserProfile me(String email) {
        return users.findByEmail(normalize(email)).map(this::profile)
                .orElseThrow(() -> new AuthenticationException(INVALID_CREDENTIALS));
    }

    /** Revokes a token until its original expiry. */
    public void logout(String token) {
        var claims = jwtService.extractClaims(token);
        blacklist.add(claims.getId(), claims.getExpiration().toInstant());
        User user = users.findByEmail(normalize(claims.getSubject())).orElse(null);
        if (user != null) audit.auth(user.getUserId(), "LOGOUT");
    }

    private UserProfile profile(User user) {
        return new UserProfile(user.getUserId(), user.getFullName(), user.getEmail(),
                user.getRole().getRoleName());
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
