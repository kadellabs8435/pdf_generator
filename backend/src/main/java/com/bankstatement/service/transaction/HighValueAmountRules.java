package com.bankstatement.service.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * FinBox-style high-value amount clustering (accepted SBI/Kotak statement profile).
 * Prefers rounded tier amounts; interest uses rare fixed decimals.
 */
final class HighValueAmountRules {

    static final int DEBIT_MAX = GeneratedStatementRules.ACTIVITY_DEBIT_MAX;
    static final int CREDIT_MAX = GeneratedStatementRules.ACTIVITY_CREDIT_MAX;

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private static final int[] ROUNDED_TIERS = {
            15_000, 16_000, 17_000, 18_500, 19_000, 20_000, 22_500, 25_000, 27_500, 30_000, 35_000, 40_000,
            50_000, 55_000, 60_000, 65_000, 70_000, 75_000, 80_000, 85_000, 90_000
    };

    private static final BigDecimal[] INTEREST_AMOUNTS = {
            new BigDecimal("142.45"),
            new BigDecimal("238.16"),
            new BigDecimal("326.83")
    };

    private HighValueAmountRules() {}

    /** Activity credits: ₹17k–₹45k. */
    static BigDecimal clusteredCredit(Random random) {
        return GeneratedStatementRules.activityCredit(random);
    }

    /** Activity debits: ₹16k–₹28k. */
    static BigDecimal clusteredDebit(Random random) {
        return GeneratedStatementRules.activityDebit(random);
    }

    static BigDecimal interestCredit(Random random) {
        return INTEREST_AMOUNTS[random.nextInt(INTEREST_AMOUNTS.length)];
    }

    static BigDecimal capToMaxWithdrawable(BigDecimal requested, BigDecimal maxWithdrawable) {
        requested = scale(requested);
        maxWithdrawable = scale(maxWithdrawable);
        if (maxWithdrawable.compareTo(ZERO) <= 0) {
            return ZERO;
        }
        if (requested.compareTo(maxWithdrawable) <= 0) {
            return requested;
        }
        return pickRoundedUpTo(maxWithdrawable);
    }

    private static BigDecimal pickRoundedInRange(Random random, int min, int max) {
        List<Integer> tiers = new ArrayList<>();
        for (int tier : ROUNDED_TIERS) {
            if (tier >= min && tier <= max) {
                tiers.add(tier);
            }
        }
        if (!tiers.isEmpty()) {
            return toRupees(tiers.get(random.nextInt(tiers.size())));
        }
        int mid = (min + max) / 2;
        int nearest = nearestTier(mid);
        if (nearest >= min && nearest <= max) {
            return toRupees(nearest);
        }
        int fallback = min + (max - min) / 2;
        fallback = (fallback / 500) * 500;
        if (fallback < min) {
            fallback = min;
        }
        if (fallback > max) {
            fallback = max;
        }
        return toRupees(fallback);
    }

    private static BigDecimal pickRoundedUpTo(BigDecimal max) {
        int maxRupees = max.intValue();
        int best = 0;
        for (int tier : ROUNDED_TIERS) {
            if (tier <= maxRupees && tier > best) {
                best = tier;
            }
        }
        if (best > 0) {
            return toRupees(best);
        }
        return scale(max);
    }

    private static int nearestTier(int rupees) {
        int best = ROUNDED_TIERS[0];
        int bestDiff = Math.abs(rupees - best);
        for (int tier : ROUNDED_TIERS) {
            int diff = Math.abs(rupees - tier);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = tier;
            }
        }
        return best;
    }

    private static BigDecimal toRupees(int rupees) {
        return new BigDecimal(rupees + ".00");
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
