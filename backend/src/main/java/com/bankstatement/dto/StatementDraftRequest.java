package com.bankstatement.dto;

import com.bankstatement.entity.*;

import java.math.BigDecimal;

public record StatementDraftRequest(
        String bankCode,
        CustomerDetails customerDetails,
        AccountDetails accountDetails,
        StatementPeriod period,
        BigDecimal openingBalance,
        TransactionSettings transactionSettings
) {}
