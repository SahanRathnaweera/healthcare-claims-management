
package com.healthcare.claimsmanagement.controller;

import com.healthcare.claimsmanagement.dto.ClaimCreateRequest;
import com.healthcare.claimsmanagement.dto.ClaimHistoryResponse;
import com.healthcare.claimsmanagement.dto.ClaimResponse;
import com.healthcare.claimsmanagement.dto.ClaimStatus;
import com.healthcare.claimsmanagement.dto.ClaimStatusUpdateRequest;
import com.healthcare.claimsmanagement.dto.ClaimUpdateRequest;
import com.healthcare.claimsmanagement.service.ClaimService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/claims")
public class ClaimController {

    private final ClaimService service;

    public ClaimController(ClaimService service) {
        this.service = service;
    }

    // Create a new claim
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimResponse create(
            @Valid @RequestBody ClaimCreateRequest request) {

        return service.create(request);
    }

    // Get claim details by ID
    @GetMapping("/{id}")
    public ClaimResponse get(@PathVariable Integer id) {

        return service.get(id);
    }

    // Search and filter claims with pagination
    @GetMapping
    public Page<ClaimResponse> search(
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) ClaimStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,
            @PageableDefault(
                    size = 20,
                    sort = "claimId",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        return service.search(
                reference,
                status,
                from,
                to,
                pageable
        );
    }

    // Update claim details
    @PutMapping("/{id}")
    public ClaimResponse update(
            @PathVariable Integer id,
            @Valid @RequestBody ClaimUpdateRequest request) {

        return service.update(id, request);
    }

    // Get complete claim status history
    @GetMapping("/{id}/history")
    public List<ClaimHistoryResponse> history(
            @PathVariable Integer id) {

        return service.getHistory(id);
    }

    // Change claim status
    // Development only: replace header with authenticated user identity.
    // Must remain disabled in shared/deployed environments
    // until authentication and transition validation are integrated.
    @PatchMapping("/{id}/status")
    public ClaimResponse status(
            @PathVariable Integer id,
            @Valid @RequestBody ClaimStatusUpdateRequest request,
            @RequestHeader(
                    value = "X-Dev-User-Id",
                    required = false
            ) Integer devActorId) {

        return service.changeStatus(
                id,
                request,
                devActorId
        );
    }
}
