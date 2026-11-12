package com.healthcare.claimsmanagement.service;

import com.healthcare.claimsmanagement.dto.*;
import com.healthcare.claimsmanagement.entity.Claim;
import com.healthcare.claimsmanagement.repository.ClaimRepository;
import com.healthcare.claimsmanagement.repository.ClaimStatusHistoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;

@Service
public class ClaimService {
    private final ClaimRepository claims;
    private final ClaimStatusHistoryRepository history;
    private final JdbcTemplate jdbc;
    private final boolean demoActorHeaderEnabled;

    public ClaimService(ClaimRepository claims, ClaimStatusHistoryRepository history,
            JdbcTemplate jdbc,
            @Value("${claims.dev-actor-header-enabled:false}") boolean demoActorHeaderEnabled) {
        this.claims = claims;
        this.history = history;
        this.jdbc = jdbc;
        this.demoActorHeaderEnabled = demoActorHeaderEnabled;
    }

    @Transactional
    public ClaimResponse create(ClaimCreateRequest request) {
        if (claims.existsByClaimReference(request.claimReference())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Claim reference already exists");
        }
        Claim claim = new Claim();
        claim.setClaimReference(request.claimReference());
        claim.setPatientRef(request.patientRef());
        claim.setClaimAmount(request.claimAmount());
        claim.setSubmissionDate(request.submissionDate() == null ? LocalDate.now() : request.submissionDate());
        // createdBy will be populated from authenticated identity after integration.
        return ClaimResponse.from(claims.saveAndFlush(claim));
    }

    @Transactional(readOnly = true)
    public ClaimResponse get(Integer id) { return ClaimResponse.from(find(id)); }

    private Claim find(Integer id) {
        return claims.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found"));
    }

    @Transactional(readOnly = true)
    public Page<ClaimResponse> search(String reference, ClaimStatus status, LocalDate from,
                                     LocalDate to, Pageable pageable) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from date must be <= to date");
        }
        Specification<Claim> spec = (root, query, cb) -> cb.conjunction();
        if (reference != null && !reference.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.like(
                cb.lower(root.get("claimReference")), "%" + reference.toLowerCase().trim() + "%"));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("currentStatus"), status.name()));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("submissionDate"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("submissionDate"), to));
        }
        return claims.findAll(spec, pageable).map(ClaimResponse::from);
    }

    @Transactional
    public ClaimResponse update(Integer id, ClaimUpdateRequest request) {
        Claim claim = find(id);
        claim.setPatientRef(request.patientRef());
        claim.setClaimAmount(request.claimAmount());
        claim.setSubmissionDate(request.submissionDate());
        return ClaimResponse.from(claims.saveAndFlush(claim));
    }

    @Transactional(readOnly = true)
    public List<ClaimHistoryResponse> getHistory(Integer id) {
        find(id);
        return history.findByClaim_ClaimIdOrderByChangedAtAscHistoryIdAsc(id)
            .stream().map(ClaimHistoryResponse::from).toList();
    }

    @Transactional
    public ClaimResponse changeStatus(Integer id, ClaimStatusUpdateRequest request, Integer devActorId) {
        if (!demoActorHeaderEnabled) {
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
                "Connect the Identity Service before enabling status changes");
        }
        if (devActorId == null || devActorId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "X-Dev-User-Id must refer to an existing demo user");
        }
        Claim before = find(id);
        if (before.getCurrentStatus().equals(request.newStatus().name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Status is unchanged");
        }
        // Proposed transitional business rules must be approved by the project lead.
        // The DB procedure MUST also enforce transitions before this endpoint is production-ready.
        // Stored procedure makes the claim update and history insert atomically.
        jdbc.update("CALL update_claim_status(?, ?, ?)", id, request.newStatus().name(), devActorId);
        // No JPA mutation: refresh via repository after clearing cached state in a separate read.
        // Return current status through a SQL projection, avoiding stale persistence context.
        return jdbc.queryForObject("SELECT claim_id, claim_reference, patient_ref, claim_amount, " +
            "submission_date, current_status, created_by, created_at, updated_at FROM claims WHERE claim_id = ?",
            (rs, rowNum) -> new ClaimResponse(rs.getInt("claim_id"), rs.getString("claim_reference"),
                rs.getString("patient_ref"), rs.getBigDecimal("claim_amount"),
                rs.getDate("submission_date").toLocalDate(), rs.getString("current_status"),
                rs.getObject("created_by", Integer.class), rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()), id);
    }
}
