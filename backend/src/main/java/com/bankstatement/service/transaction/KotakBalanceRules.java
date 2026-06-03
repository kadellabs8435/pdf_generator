package com.bankstatement.service.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Kotak-only running balance band (isolated from SBI/BOI floor logic). */
final class KotakBalanceRules {

    static final BigDecimal MIN = GeneratedStatementRules.MIN_RUNNING_BALANCE;
    static final BigDecimal MAX = GeneratedStatementRules.MAX_RUNNING_BALANCE;

    private KotakBalanceRules() {}

    static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
