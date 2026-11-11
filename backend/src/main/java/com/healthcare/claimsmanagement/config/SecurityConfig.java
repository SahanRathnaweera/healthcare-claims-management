package com.healthcare.claimsmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;
import com.healthcare.claimsmanagement.security.AccessPolicy;
import com.healthcare.claimsmanagement.security.SecurityErrorHandler;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private final com.healthcare.claimsmanagement.security.JwtAuthenticationFilter jwtFilter;
    private final SecurityErrorHandler securityErrorHandler;

    public SecurityConfig(com.healthcare.claimsmanagement.security.JwtAuthenticationFilter jwtFilter,
                          SecurityErrorHandler securityErrorHandler) {
        this.jwtFilter = jwtFilter;
        this.securityErrorHandler = securityErrorHandler;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/me", "/api/auth/logout",
                                "/api/chatbot/**").authenticated()
                        .requestMatchers("/api/users/**", "/api/roles/**").hasAuthority(AccessPolicy.ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/claims/**", "/api/denials/**",
                                "/api/follow-up-tasks/**", "/api/payments/**",
                                "/api/dashboard/**").hasAnyAuthority(
                                AccessPolicy.ADMIN, AccessPolicy.CLAIMS, AccessPolicy.DENIALS,
                                AccessPolicy.MANAGER)
                        .requestMatchers(HttpMethod.GET, "/api/audit-logs/**", "/api/reports/**")
                        .hasAnyAuthority(AccessPolicy.ADMIN, AccessPolicy.MANAGER)
                        .requestMatchers(HttpMethod.POST, "/api/claims/**")
                        .hasAnyAuthority(AccessPolicy.ADMIN, AccessPolicy.CLAIMS)
                        .requestMatchers(HttpMethod.PUT, "/api/claims/*/status")
                        .hasAnyAuthority(AccessPolicy.ADMIN, AccessPolicy.CLAIMS, AccessPolicy.DENIALS)
                        .requestMatchers(HttpMethod.PUT, "/api/claims/**")
                        .hasAnyAuthority(AccessPolicy.ADMIN, AccessPolicy.CLAIMS)
                        .requestMatchers(HttpMethod.PATCH, "/api/claims/*/status")
                        .hasAnyAuthority(AccessPolicy.ADMIN, AccessPolicy.CLAIMS, AccessPolicy.DENIALS)
                        .requestMatchers(HttpMethod.PATCH, "/api/claims/**")
                        .hasAnyAuthority(AccessPolicy.ADMIN, AccessPolicy.CLAIMS)
                        .requestMatchers(HttpMethod.POST, "/api/denials/**",
                                "/api/follow-up-tasks/**")
                        .hasAnyAuthority(AccessPolicy.ADMIN, AccessPolicy.DENIALS)
                        .requestMatchers(HttpMethod.PUT, "/api/denials/**",
                                "/api/follow-up-tasks/**")
                        .hasAnyAuthority(AccessPolicy.ADMIN, AccessPolicy.CLAIMS, AccessPolicy.DENIALS)
                        .requestMatchers(HttpMethod.PATCH, "/api/denials/**",
                                "/api/follow-up-tasks/**")
                        .hasAnyAuthority(AccessPolicy.ADMIN, AccessPolicy.CLAIMS, AccessPolicy.DENIALS)
                        .requestMatchers(HttpMethod.POST, "/api/payments/**")
                        .hasAnyAuthority(AccessPolicy.ADMIN, AccessPolicy.CLAIMS)
                        .requestMatchers(HttpMethod.PUT, "/api/payments/**")
                        .hasAnyAuthority(AccessPolicy.ADMIN, AccessPolicy.CLAIMS)
                        .anyRequest().denyAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}