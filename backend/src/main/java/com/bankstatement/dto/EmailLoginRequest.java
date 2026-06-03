package com.bankstatement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailLoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {}
