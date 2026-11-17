package com.healthcare.claimsmanagement.repository;

import com.healthcare.claimsmanagement.entity.ClaimStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClaimStatusHistoryRepository extends JpaRepository<ClaimStatusHistory, Integer> {
    List<ClaimStatusHistory> findByClaim_ClaimIdOrderByChangedAtAscHistoryIdAsc(Integer claimId);
}
