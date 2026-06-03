package com.bankstatement.dto;

public record BankTemplateRequest(
        String code,
        String displayName,
        boolean active
) {}
