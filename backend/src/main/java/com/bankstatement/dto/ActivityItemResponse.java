package com.bankstatement.dto;

import java.time.Instant;

public record ActivityItemResponse(
        String id,
        String userName,
        String action,
        String details,
        Instant createdAt
) {}
