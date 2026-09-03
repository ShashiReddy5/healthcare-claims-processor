package com.shashireddy.claims.dto;

import com.shashireddy.claims.model.Claim;
import com.shashireddy.claims.model.ClaimStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public class ClaimDtos {

    public record ClaimSubmissionRequest(
            @NotBlank String memberId,
            @NotBlank String diagnosisCode,
            @NotNull @DecimalMin(value = "0.01") BigDecimal billedAmount
    ) {
    }

    public record ClaimResponse(
            String claimId,
            String memberId,
            String diagnosisCode,
            BigDecimal billedAmount,
            BigDecimal allowedAmount,
            ClaimStatus status,
            String denialReason,
            Instant submittedAt,
            Instant adjudicatedAt
    ) {
        public static ClaimResponse from(Claim claim, String decryptedMemberId, String decryptedDiagnosisCode) {
            return new ClaimResponse(
                    claim.getClaimId(),
                    decryptedMemberId,
                    decryptedDiagnosisCode,
                    claim.getBilledAmount(),
                    claim.getAllowedAmount(),
                    claim.getStatus(),
                    claim.getDenialReason(),
                    claim.getSubmittedAt(),
                    claim.getAdjudicatedAt()
            );
        }
    }
}
