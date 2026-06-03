package com.bankstatement.dto;

public record BankTemplateResponse(
        String id,
        String code,
        String displayName,
        boolean active
) {}
