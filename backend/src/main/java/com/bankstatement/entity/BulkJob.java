package com.bankstatement.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bulk_jobs")
public class BulkJob {

    @Id
    private String id;

    private String userId;

    private String status;

    private int totalRows;

    private int processedRows;

    private int successCount;

    private int failureCount;

    private String errorReport;

    @CreatedDate
    private Instant createdAt;

    private Instant completedAt;
}
