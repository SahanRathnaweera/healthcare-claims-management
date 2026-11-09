package com.healthcare.claimsmanagement.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenBlacklist {
    private final ConcurrentHashMap<String, Instant> tokens = new ConcurrentHashMap<>();

    public void add(String jti, Instant expiry) {
        tokens.put(jti, expiry);
    }

    public boolean contains(String jti) {
        Instant expiry = tokens.get(jti);
        return expiry != null && expiry.isAfter(Instant.now());
    }

    @Scheduled(fixedRate = 60_000)
    void removeExpired() {
        Instant now = Instant.now();
        tokens.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
    }
}
