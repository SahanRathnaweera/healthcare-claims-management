package com.healthcare.claimsmanagement.security;

import com.healthcare.claimsmanagement.common.RoleName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RbacPolicyTest {
    @Test
    void mapsEveryDatabaseRoleToItsRoleAuthority() {
        assertEquals("ROLE_SYSTEM_ADMINISTRATOR",
                RoleName.fromDatabaseName("System Administrator").authority());
        assertEquals("ROLE_CLAIMS_PROCESSING_OFFICER",
                RoleName.fromDatabaseName("Claims Processing Officer").authority());
        assertEquals("ROLE_DENIAL_MANAGEMENT_OFFICER",
                RoleName.fromDatabaseName("Denial Management Officer").authority());
        assertEquals("ROLE_MANAGER_ANALYST",
                RoleName.fromDatabaseName("Manager/Analyst").authority());
    }

    @Test
    void centralPolicyUsesTheMappedAuthorities() {
        assertEquals(RoleName.SYSTEM_ADMINISTRATOR.authority(), AccessPolicy.ADMIN);
        assertEquals(RoleName.CLAIMS_PROCESSING_OFFICER.authority(), AccessPolicy.CLAIMS);
        assertEquals(RoleName.DENIAL_MANAGEMENT_OFFICER.authority(), AccessPolicy.DENIALS);
        assertEquals(RoleName.MANAGER_ANALYST.authority(), AccessPolicy.MANAGER);
    }
}
