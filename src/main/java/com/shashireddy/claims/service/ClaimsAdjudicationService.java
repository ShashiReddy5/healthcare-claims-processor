package com.shashireddy.claims.service;

import com.shashireddy.claims.crypto.PhiEncryptionService;
import com.shashireddy.claims.dto.ClaimDtos.ClaimResponse;
import com.shashireddy.claims.dto.ClaimDtos.ClaimSubmissionRequest;
import com.shashireddy.claims.model.Claim;
import com.shashireddy.claims.model.ClaimStatus;
import com.shashireddy.claims.repository.ClaimRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Applies a simple set of adjudication rules to an incoming claim.
 *
 * This stands in for a real payer rules engine: eligibility checks,
 * contracted rates, prior-authorization lookups, and coordination-of-benefits
 * logic are all out of scope here (see the README). What's here is a real,
 * end-to-end decision path with PHI encrypted before it's persisted.
 */
@Service
public class ClaimsAdjudicationService {

    static final BigDecimal AUTO_APPROVE_LIMIT = new BigDecimal("500.00");
    static final BigDecimal DENIAL_THRESHOLD = new BigDecimal("50000.00");

    private final ClaimRepository claimRepository;
    private final PhiEncryptionService encryptionService;
    private final AuditLogger auditLogger;

    public ClaimsAdjudicationService(ClaimRepository claimRepository,
                                      PhiEncryptionService encryptionService,
                                      AuditLogger auditLogger) {
        this.claimRepository = claimRepository;
        this.encryptionService = encryptionService;
        this.auditLogger = auditLogger;
    }

    public ClaimResponse adjudicate(ClaimSubmissionRequest request) {
        String claimId = "CLM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        auditLogger.log("CLAIM_RECEIVED", claimId, currentActor());

        Claim claim = new Claim(
                UUID.randomUUID().toString(),
                claimId,
                encryptionService.encrypt(request.memberId()),
                encryptionService.encrypt(request.diagnosisCode()),
                request.billedAmount(),
                ClaimStatus.PENDING_REVIEW,
                Instant.now()
        );

        applyRules(claim, request.billedAmount());
        claim.setAdjudicatedAt(Instant.now());

        Claim saved = claimRepository.save(claim);
        auditLogger.log("CLAIM_" + saved.getStatus(), claimId, currentActor());

        return toResponse(saved);
    }

    private void applyRules(Claim claim, BigDecimal billedAmount) {
        if (billedAmount.compareTo(DENIAL_THRESHOLD) > 0) {
            claim.setStatus(ClaimStatus.DENIED);
            claim.setDenialReason("Billed amount exceeds policy limit of " + DENIAL_THRESHOLD);
            claim.setAllowedAmount(BigDecimal.ZERO);
        } else if (billedAmount.compareTo(AUTO_APPROVE_LIMIT) <= 0) {
            claim.setStatus(ClaimStatus.APPROVED);
            claim.setAllowedAmount(billedAmount);
        } else {
            claim.setStatus(ClaimStatus.PENDING_REVIEW);
            claim.setAllowedAmount(null);
        }
    }

    public ClaimResponse getByClaimId(String claimId) {
        Claim claim = claimRepository.findByClaimId(claimId)
                .orElseThrow(() -> new NoSuchElementException("No claim found with id " + claimId));
        return toResponse(claim);
    }

    public Page<ClaimResponse> list(Pageable pageable) {
        return claimRepository.findAll(pageable).map(this::toResponse);
    }

    public Page<ClaimResponse> listByStatus(ClaimStatus status, Pageable pageable) {
        return claimRepository.findByStatus(status, pageable).map(this::toResponse);
    }

    private ClaimResponse toResponse(Claim claim) {
        return ClaimResponse.from(
                claim,
                encryptionService.decrypt(claim.getMemberIdEncrypted()),
                encryptionService.decrypt(claim.getDiagnosisCodeEncrypted())
        );
    }

    private String currentActor() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
