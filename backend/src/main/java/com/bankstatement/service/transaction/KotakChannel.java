package com.bankstatement.service.transaction;

/** Payment channel for FinBox-accepted Kotak narration mix. */
enum KotakChannel {
    UPI_DEBIT,
    UPI_CREDIT,
    UPI_CR,
    UPI_REV,
    IMPS,
    NEFT,
    INT_CREDIT
}
