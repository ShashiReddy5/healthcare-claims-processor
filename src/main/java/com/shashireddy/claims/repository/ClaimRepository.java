package com.shashireddy.claims.repository;

import com.shashireddy.claims.model.Claim;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClaimRepository extends JpaRepository<Claim, String> {

    Optional<Claim> findByClaimId(String claimId);

    Page<Claim> findByStatus(com.shashireddy.claims.model.ClaimStatus status, Pageable pageable);
}
