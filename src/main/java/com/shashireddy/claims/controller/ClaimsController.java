package com.shashireddy.claims.controller;

import com.shashireddy.claims.dto.ClaimDtos.ClaimResponse;
import com.shashireddy.claims.dto.ClaimDtos.ClaimSubmissionRequest;
import com.shashireddy.claims.model.ClaimStatus;
import com.shashireddy.claims.service.ClaimsAdjudicationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/claims")
public class ClaimsController {

    private final ClaimsAdjudicationService service;

    public ClaimsController(ClaimsAdjudicationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ClaimResponse> submit(@Valid @RequestBody ClaimSubmissionRequest request) {
        return ResponseEntity.ok(service.adjudicate(request));
    }

    @GetMapping("/{claimId}")
    public ClaimResponse getByClaimId(@PathVariable String claimId) {
        return service.getByClaimId(claimId);
    }

    @GetMapping
    public Page<ClaimResponse> list(
            @RequestParam(required = false) ClaimStatus status,
            @PageableDefault(size = 20) Pageable pageable) {

        return status == null ? service.list(pageable) : service.listByStatus(status, pageable);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
    }
}
