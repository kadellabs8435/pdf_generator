package com.bankstatement.repository;

import com.bankstatement.entity.OtpPurpose;
import com.bankstatement.entity.OtpToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OtpTokenRepository extends MongoRepository<OtpToken, String> {
    Optional<OtpToken> findByChallengeTokenAndPurposeAndUsedFalse(String challengeToken, OtpPurpose purpose);
}
