package com.bankstatement.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "statements")
public class Statement {

    @Id
    private String id;

    @Indexed
    private String createdByUserId;

    private String bankCode;

    private StatementStatus status;

    private CustomerDetails customerDetails;

    private AccountDetails accountDetails;

    private StatementPeriod period;

    private BigDecimal openingBalance;

    private BigDecimal closingBalance;

    private TransactionSettings transactionSettings;

    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();

    private String pdfPath;

    private String batchId;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
