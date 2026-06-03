package com.bankstatement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MobileLoginRequest(
        @NotBlank @Pattern(regexp = "^\\d{10}$") String mobile,
        @NotBlank String password
) {}
