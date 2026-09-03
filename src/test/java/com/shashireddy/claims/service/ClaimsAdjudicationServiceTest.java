package com.shashireddy.claims.service;

import com.shashireddy.claims.crypto.PhiEncryptionService;
import com.shashireddy.claims.dto.ClaimDtos.ClaimResponse;
import com.shashireddy.claims.dto.ClaimDtos.ClaimSubmissionRequest;
import com.shashireddy.claims.model.Claim;
import com.shashireddy.claims.model.ClaimStatus;
import com.shashireddy.claims.repository.ClaimRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClaimsAdjudicationServiceTest {

    private ClaimRepository repository;
    private PhiEncryptionService encryptionService;
    private AuditLogger auditLogger;
    private ClaimsAdjudicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(ClaimRepository.class);
        // Use the real encryption service so the encrypt/decrypt round trip
        // is exercised for real, not mocked away.
        encryptionService = new PhiEncryptionService("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        auditLogger = mock(AuditLogger.class);
        service = new ClaimsAdjudicationService(repository, encryptionService, auditLogger);

        when(repository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void smallClaimIsAutoApprovedForTheFullBilledAmount() {
        ClaimSubmissionRequest request = new ClaimSubmissionRequest(
                "member-123", "E11.9", new BigDecimal("120.00"));

        ClaimResponse result = service.adjudicate(request);

        assertThat(result.status()).isEqualTo(ClaimStatus.APPROVED);
        assertThat(result.allowedAmount()).isEqualByComparingTo("120.00");
    }

    @Test
    void midSizedClaimIsPendedForManualReview() {
        ClaimSubmissionRequest request = new ClaimSubmissionRequest(
                "member-123", "M54.5", new BigDecimal("2500.00"));

        ClaimResponse result = service.adjudicate(request);

        assertThat(result.status()).isEqualTo(ClaimStatus.PENDING_REVIEW);
        assertThat(result.allowedAmount()).isNull();
    }

    @Test
    void claimOverThePolicyLimitIsDenied() {
        ClaimSubmissionRequest request = new ClaimSubmissionRequest(
                "member-123", "S72.001A", new BigDecimal("75000.00"));

        ClaimResponse result = service.adjudicate(request);

        assertThat(result.status()).isEqualTo(ClaimStatus.DENIED);
        assertThat(result.denialReason()).contains("exceeds policy limit");
    }

    @Test
    void memberIdIsNeverPersistedInPlainText() {
        ClaimSubmissionRequest request = new ClaimSubmissionRequest(
                "member-super-secret", "E11.9", new BigDecimal("50.00"));

        service.adjudicate(request);

        ArgumentCaptor<Claim> captor = ArgumentCaptor.forClass(Claim.class);
        verify(repository).save(captor.capture());

        Claim saved = captor.getValue();
        assertThat(saved.getMemberIdEncrypted()).doesNotContain("member-super-secret");
        assertThat(encryptionService.decrypt(saved.getMemberIdEncrypted())).isEqualTo("member-super-secret");
    }
}
