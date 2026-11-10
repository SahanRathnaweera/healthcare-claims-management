package com.healthcare.claimsmanagement.common;

import java.util.Arrays;

public enum RoleName {
    SYSTEM_ADMINISTRATOR("System Administrator", "ROLE_SYSTEM_ADMINISTRATOR"),
    CLAIMS_PROCESSING_OFFICER("Claims Processing Officer", "ROLE_CLAIMS_PROCESSING_OFFICER"),
    DENIAL_MANAGEMENT_OFFICER("Denial Management Officer", "ROLE_DENIAL_MANAGEMENT_OFFICER"),
    MANAGER_ANALYST("Manager/Analyst", "ROLE_MANAGER_ANALYST");

    private final String databaseName;
    private final String authority;

    RoleName(String databaseName, String authority) {
        this.databaseName = databaseName;
        this.authority = authority;
    }

    public String databaseName() {
        return databaseName;
    }

    public String authority() {
        return authority;
    }

    public static RoleName fromDatabaseName(String name) {
        return Arrays.stream(values())
                .filter(role -> role.databaseName.equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown role: " + name));
    }
}
