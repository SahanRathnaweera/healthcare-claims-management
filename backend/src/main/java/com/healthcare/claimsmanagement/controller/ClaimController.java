package com.healthcare.claimsmanagement.controller;

import com.healthcare.claimsmanagement.dto.*;
import com.healthcare.claimsmanagement.service.ClaimService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/claims")
public class ClaimController {
    private final ClaimService service;
    public ClaimController(ClaimService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimResponse create(@Valid @RequestBody ClaimCreateRequest request) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    public ClaimResponse get(@PathVariable Integer id) { return service.get(id); }

    @GetMapping
    public Page<ClaimResponse> search(
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) ClaimStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "claimId", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.search(reference, status, from, to, pageable);
    }

    @PutMapping("/{id}")
    public ClaimResponse update(@PathVariable Integer id, @Valid @RequestBody ClaimUpdateRequest request) {
        return service.update(id, request);
    }

    @GetMapping("/{id}/history")
    public List<ClaimHistoryResponse> history(@PathVariable Integer id) { return service.getHistory(id); }

    // Development-only actor header: DO NOT enable for shared/deployed environments.
    @PatchMapping("/{id}/status")
    public ClaimResponse status(@PathVariable Integer id,
            @Valid @RequestBody ClaimStatusUpdateRequest request,
            @RequestHeader(value = "X-Dev-User-Id", required = false) Integer devActorId) {
        return service.changeStatus(id, request, devActorId);
    }
}
