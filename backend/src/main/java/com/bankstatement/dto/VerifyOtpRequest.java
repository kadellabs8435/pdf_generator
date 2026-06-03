package com.bankstatement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(
        @NotBlank String challengeToken,
        @NotBlank @Pattern(regexp = "^\\d{6}$") String otp
) {}
