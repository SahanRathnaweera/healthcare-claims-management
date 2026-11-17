
package com.healthcare.claimsmanagement.service;

import com.healthcare.claimsmanagement.dto.*;
import com.healthcare.claimsmanagement.entity.Claim;
import com.healthcare.claimsmanagement.repository.ClaimRepository;
import com.healthcare.claimsmanagement.repository.ClaimStatusHistoryRepository;

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

    public ClaimService(
            ClaimRepository claims,
            ClaimStatusHistoryRepository history,
            JdbcTemplate jdbc) {

        this.claims = claims;
        this.history = history;
        this.jdbc = jdbc;
    }

    // Create a new claim
    @Transactional
    public ClaimResponse create(ClaimCreateRequest request) {

        if (claims.existsByClaimReference(request.claimReference())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Claim reference already exists");
        }

        Claim claim = new Claim();

        claim.setClaimReference(request.claimReference());
        claim.setPatientRef(request.patientRef());
        claim.setClaimAmount(request.claimAmount());

        claim.setSubmissionDate(
                request.submissionDate() == null
                        ? LocalDate.now()
                        : request.submissionDate()
        );

        // TODO: Populate createdBy using authenticated
        // identity in the claim creation flow.

        return ClaimResponse.from(claims.saveAndFlush(claim));
    }

    // Get claim details by ID
    @Transactional(readOnly = true)
    public ClaimResponse get(Integer id) {
        return ClaimResponse.from(find(id));
    }

    private Claim find(Integer id) {

        return claims.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Claim not found"));
    }

    // Search and filter claims
    @Transactional(readOnly = true)
    public Page<ClaimResponse> search(
            String reference,
            ClaimStatus status,
            LocalDate from,
            LocalDate to,
            Pageable pageable) {

        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "from date must be <= to date");
        }

        Specification<Claim> spec =
                (root, query, cb) -> cb.conjunction();

        if (reference != null && !reference.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(
                            cb.lower(root.get("claimReference")),
                            "%" + reference.toLowerCase().trim() + "%"
                    )
            );
        }

        if (status != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(
                            root.get("currentStatus"),
                            status.name()
                    )
            );
        }

        if (from != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(
                            root.get("submissionDate"), from)
            );
        }

        if (to != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(
                            root.get("submissionDate"), to)
            );
        }

        return claims.findAll(spec, pageable)
                .map(ClaimResponse::from);
    }

    // Update claim details
    @Transactional
    public ClaimResponse update(
            Integer id,
            ClaimUpdateRequest request) {

        Claim claim = find(id);

        claim.setPatientRef(request.patientRef());
        claim.setClaimAmount(request.claimAmount());
        claim.setSubmissionDate(request.submissionDate());

        return ClaimResponse.from(claims.saveAndFlush(claim));
    }

    // Retrieve complete claim status history
    @Transactional(readOnly = true)
    public List<ClaimHistoryResponse> getHistory(Integer id) {

        find(id);

        return history
                .findByClaim_ClaimIdOrderByChangedAtAscHistoryIdAsc(id)
                .stream()
                .map(ClaimHistoryResponse::from)
                .toList();
    }

    // Change claim status using JWT authenticated actor ID
    @Transactional
    public ClaimResponse changeStatus(
            Integer id,
            ClaimStatusUpdateRequest request,
            Integer actorId) {

        if (actorId == null || actorId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authenticated user is required");
        }

        Claim before = find(id);

        String oldStatus = before.getCurrentStatus();
        String newStatus = request.newStatus().name();

        if (oldStatus.equals(newStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Status is unchanged");
        }

        // Database procedure validates:
        // - Responsible user exists and is active
        // - New status is valid
        // - Claim exists
        // - Status transition is allowed
        // - Status history is recorded

        jdbc.update(
                "CALL update_claim_status(?, ?, ?)",
                id,
                newStatus,
                actorId
        );

        // Read directly from SQL to avoid stale JPA state
        // after the stored procedure updates the claim.

        return jdbc.queryForObject(
                "SELECT claim_id, claim_reference, patient_ref, " +
                "claim_amount, submission_date, current_status, " +
                "created_by, created_at, updated_at " +
                "FROM claims WHERE claim_id = ?",

                (rs, rowNum) -> new ClaimResponse(
                        rs.getInt("claim_id"),
                        rs.getString("claim_reference"),
                        rs.getString("patient_ref"),
                        rs.getBigDecimal("claim_amount"),
                        rs.getDate("submission_date").toLocalDate(),
                        rs.getString("current_status"),
                        rs.getObject("created_by", Integer.class),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("updated_at").toLocalDateTime()
                ),
                id
        );
    }
}
