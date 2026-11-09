package com.healthcare.claimsmanagement.repository;

import com.healthcare.claimsmanagement.entity.Claim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;

public interface ClaimRepository extends JpaRepository<Claim, Integer>, JpaSpecificationExecutor<Claim> {
    boolean existsByClaimReference(String claimReference);
    Optional<Claim> findByClaimReference(String claimReference);
}
