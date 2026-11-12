package com.healthcare.claimsmanagement.security;

import com.healthcare.claimsmanagement.common.RoleName;

/**
 * Central authorization vocabulary and URL policy. Service-level ownership and
 * denial-related status checks remain the responsibility of those services.
 */
public final class AccessPolicy {
    public static final String ADMIN = RoleName.SYSTEM_ADMINISTRATOR.authority();
    public static final String CLAIMS = RoleName.CLAIMS_PROCESSING_OFFICER.authority();
    public static final String DENIALS = RoleName.DENIAL_MANAGEMENT_OFFICER.authority();
    public static final String MANAGER = RoleName.MANAGER_ANALYST.authority();

    public static final String ADMIN_OR_CLAIMS = "hasAnyAuthority('" + ADMIN + "','" + CLAIMS + "')";
    public static final String ADMIN_OR_DENIALS = "hasAnyAuthority('" + ADMIN + "','" + DENIALS + "')";
    public static final String ADMIN_OR_MANAGER = "hasAnyAuthority('" + ADMIN + "','" + MANAGER + "')";

    private AccessPolicy() {
    }

    /**
     * Service-layer enforcement is required for task ownership and
     * denial-related claim status changes.
     */
    public static final String OWNERSHIP_RULES_DEFERRED_TO_SERVICES =
            "Enforce ownership and denial-related status checks in service methods.";
}
