package com.bankstatement.service.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Amount clustering for FinBox-accepted Kotak statements: mostly ₹15k–₹39k,
 * occasional ₹40k, rare ₹45k; avoids ₹50k+.
 */
final class KotakAcceptedAmountRules {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private static final BigDecimal[] INTEREST_AMOUNTS = {
            new BigDecimal("270.98"),
            new BigDecimal("344.81"),
            new BigDecimal("412.35"),
            new BigDecimal("198.47"),
            new BigDecimal("326.50")
    };

    private KotakAcceptedAmountRules() {}

    static final BigDecimal ACTIVITY_MIN = GeneratedStatementRules.ACTIVITY_DEBIT_MIN_BD;
    static final BigDecimal ACTIVITY_CREDIT_MIN = GeneratedStatementRules.ACTIVITY_CREDIT_MIN_BD;

    static BigDecimal activityCreditAmount(Random random) {
        return GeneratedStatementRules.activityCredit(random);
    }

    static BigDecimal activityDebitAmount(Random random) {
        return GeneratedStatementRules.activityDebit(random);
    }

    /** @deprecated use {@link #activityCreditAmount} or {@link #activityDebitAmount} */
    static BigDecimal activityAmount(Random random) {
        return activityCreditAmount(random);
    }

    static BigDecimal interestAmount(Random random) {
        return INTEREST_AMOUNTS[random.nextInt(INTEREST_AMOUNTS.length)];
    }

    static String interestNarration(BigDecimal amount) {
        return "INT. CREDIT " + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    static BigDecimal capToMaxWithdrawable(BigDecimal requested, BigDecimal maxWithdrawable, Random random) {
        requested = scale(requested);
        maxWithdrawable = scale(maxWithdrawable);
        if (maxWithdrawable.compareTo(ZERO) <= 0) {
            return ZERO;
        }
        if (requested.compareTo(maxWithdrawable) <= 0) {
            return requested;
        }
        int cap = Math.min(GeneratedStatementRules.ACTIVITY_DEBIT_MAX, maxWithdrawable.intValue());
        if (cap < GeneratedStatementRules.ACTIVITY_DEBIT_MIN) {
            return scale(maxWithdrawable);
        }
        return RealisticBankAmount.generateWholeRupeesNaturalUpTo(random, new BigDecimal(cap));
    }

    private static BigDecimal roundedTier(Random random, int min, int max) {
        int[] tiers = {15_000, 16_500, 18_000, 19_500, 21_000, 22_500, 25_000, 27_500, 30_000, 32_500, 35_000, 37_500, 39_000};
        List<Integer> inRange = new ArrayList<>();
        for (int tier : tiers) {
            if (tier >= min && tier <= max) {
                inRange.add(tier);
            }
        }
        if (inRange.isEmpty()) {
            return RealisticBankAmount.generateWholeRupeesNatural(random, min, max);
        }
        return toRupees(inRange.get(random.nextInt(inRange.size())));
    }

    private static BigDecimal toRupees(int rupees) {
        return new BigDecimal(rupees + ".00");
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
