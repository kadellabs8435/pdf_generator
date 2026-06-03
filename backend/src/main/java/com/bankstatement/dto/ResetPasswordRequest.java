package com.bankstatement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(
        @NotBlank String challengeToken,
        @NotBlank @Pattern(regexp = "^\\d{6}$") String otp,
        @NotBlank String newPassword
) {}
