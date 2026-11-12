package com.healthcare.claimsmanagement.security;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class RbacSecurityTestController {
    @GetMapping("/claims")
    ResponseEntity<Void> claims() {
        return ResponseEntity.ok().build();
    }

    @RequestMapping({"/users", "/users/{id}", "/roles", "/roles/{id}",
            "/claims", "/claims/{id}", "/claims/{id}/status", "/denials", "/denials/{id}",
            "/follow-up-tasks", "/follow-up-tasks/{id}", "/payments", "/payments/{id}",
            "/audit-logs", "/audit-logs/{id}", "/dashboard", "/reports", "/chatbot"})
    ResponseEntity<Void> endpoint() {
        return ResponseEntity.ok().build();
    }
}
