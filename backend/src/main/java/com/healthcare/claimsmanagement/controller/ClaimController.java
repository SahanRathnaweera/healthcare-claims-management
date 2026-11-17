
package com.healthcare.claimsmanagement.controller;

import com.healthcare.claimsmanagement.dto.ClaimCreateRequest;
import com.healthcare.claimsmanagement.dto.ClaimHistoryResponse;
import com.healthcare.claimsmanagement.dto.ClaimResponse;
import com.healthcare.claimsmanagement.dto.ClaimStatus;
import com.healthcare.claimsmanagement.dto.ClaimStatusUpdateRequest;
import com.healthcare.claimsmanagement.dto.ClaimUpdateRequest;

import com.healthcare.claimsmanagement.service.ClaimService;
import com.healthcare.claimsmanagement.service.AuthenticatedActorResolver;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/claims")
public class ClaimController {

    private static final Logger log =
            LoggerFactory.getLogger(ClaimController.class);

    private final ClaimService service;
    private final AuthenticatedActorResolver actorResolver;

    public ClaimController(
            ClaimService service,
            AuthenticatedActorResolver actorResolver) {

        this.service = service;
        this.actorResolver = actorResolver;
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

    // Change claim status using authenticated JWT user identity
    @PatchMapping("/{id}/status")
    public ClaimResponse status(
            @PathVariable Integer id,
            @Valid @RequestBody ClaimStatusUpdateRequest request,
            Authentication authentication) {

        Integer actorId =
                actorResolver.resolveUserId(authentication);

        log.info(
                "Claim status update requested: claimId={}, actorId={}",
                id,
                actorId
        );

        return service.changeStatus(
                id,
                request,
                actorId
        );
    }
}
