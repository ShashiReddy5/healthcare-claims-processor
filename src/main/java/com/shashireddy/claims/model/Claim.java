package com.shashireddy.claims.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "claims")
public class Claim {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String claimId;

    /**
     * Member/patient identifier, stored only in its AES-GCM encrypted form.
     * See {@link com.shashireddy.claims.crypto.PhiEncryptionService}.
     */
    @Column(nullable = false, length = 512)
    private String memberIdEncrypted;

    @Column(nullable = false, length = 512)
    private String diagnosisCodeEncrypted;

    @Column(nullable = false)
    private BigDecimal billedAmount;

    private BigDecimal allowedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus status;

    private String denialReason;

    @Column(nullable = false)
    private Instant submittedAt;

    private Instant adjudicatedAt;

    protected Claim() {
        // for JPA
    }

    public Claim(String id, String claimId, String memberIdEncrypted, String diagnosisCodeEncrypted,
                 BigDecimal billedAmount, ClaimStatus status, Instant submittedAt) {
        this.id = id;
        this.claimId = claimId;
        this.memberIdEncrypted = memberIdEncrypted;
        this.diagnosisCodeEncrypted = diagnosisCodeEncrypted;
        this.billedAmount = billedAmount;
        this.status = status;
        this.submittedAt = submittedAt;
    }

    public String getId() {
        return id;
    }

    public String getClaimId() {
        return claimId;
    }

    public String getMemberIdEncrypted() {
        return memberIdEncrypted;
    }

    public String getDiagnosisCodeEncrypted() {
        return diagnosisCodeEncrypted;
    }

    public BigDecimal getBilledAmount() {
        return billedAmount;
    }

    public BigDecimal getAllowedAmount() {
        return allowedAmount;
    }

    public void setAllowedAmount(BigDecimal allowedAmount) {
        this.allowedAmount = allowedAmount;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimStatus status) {
        this.status = status;
    }

    public String getDenialReason() {
        return denialReason;
    }

    public void setDenialReason(String denialReason) {
        this.denialReason = denialReason;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getAdjudicatedAt() {
        return adjudicatedAt;
    }

    public void setAdjudicatedAt(Instant adjudicatedAt) {
        this.adjudicatedAt = adjudicatedAt;
    }
}
