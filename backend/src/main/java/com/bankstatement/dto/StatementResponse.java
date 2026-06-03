package com.bankstatement.dto;

import com.bankstatement.entity.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record StatementResponse(
        String id,
        String bankCode,
        StatementStatus status,
        CustomerDetails customerDetails,
        AccountDetails accountDetails,
        StatementPeriod period,
        BigDecimal openingBalance,
        BigDecimal closingBalance,
        TransactionSettings transactionSettings,
        List<Transaction> transactions,
        String pdfPath,
        String createdByUserId,
        Instant createdAt,
        Instant updatedAt
) {}
