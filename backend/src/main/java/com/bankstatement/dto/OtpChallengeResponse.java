package com.bankstatement.dto;

public record OtpChallengeResponse(
        String challengeToken,
        String message
) {}
