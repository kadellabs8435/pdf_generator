package com.bankstatement.dto;

import java.time.Instant;

public record BulkJobResponse(
        String id,
        String status,
        int totalRows,
        int processedRows,
        int successCount,
        int failureCount,
        String errorReport,
        Instant createdAt,
        Instant completedAt
) {}
