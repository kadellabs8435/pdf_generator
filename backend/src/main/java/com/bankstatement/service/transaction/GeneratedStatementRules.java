package com.bankstatement.service.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

/**
 * Shared limits for generated statement activity (amounts and running balance band).
 */
final class GeneratedStatementRules {

    static final BigDecimal MIN_RUNNING_BALANCE = new BigDecimal("70000.00");
    static final BigDecimal MAX_RUNNING_BALANCE = new BigDecimal("120000.00");

    static final int ACTIVITY_CREDIT_MIN = 17_000;
    static final int ACTIVITY_CREDIT_MAX = 45_000;
    static final int ACTIVITY_DEBIT_MIN = 16_000;
    static final int ACTIVITY_DEBIT_MAX = 28_000;

    static final BigDecimal ACTIVITY_CREDIT_MIN_BD = new BigDecimal("17000.00");
    static final BigDecimal ACTIVITY_DEBIT_MIN_BD = new BigDecimal("16000.00");

    private GeneratedStatementRules() {}

    static BigDecimal activityCredit(Random random) {
        return RealisticBankAmount.generateWholeRupeesNatural(random, ACTIVITY_CREDIT_MIN, ACTIVITY_CREDIT_MAX);
    }

    static BigDecimal activityDebit(Random random) {
        return RealisticBankAmount.generateWholeRupeesNatural(random, ACTIVITY_DEBIT_MIN, ACTIVITY_DEBIT_MAX);
    }

    static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
