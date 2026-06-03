package com.bankstatement.service.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

/**
 * Produces bank-statement-like amounts: mostly whole rupees, occasional .50/.75/.90,
 * rare realistic paise — always exactly 2 decimal places.
 */
final class RealisticBankAmount {

    private static final int[] CENT_COMMON = {50, 25, 75, 90};
    private static final int[] CENT_SPECIAL = {7, 35, 12, 57, 3, 15, 20, 99, 5, 10, 80, 60};
    private static final int[] NICE_RUPEES = {
            10, 22, 35, 50, 65, 70, 100, 110, 120, 180, 200, 225, 250, 300, 350, 400, 450, 500,
            600, 750, 820, 999, 1200, 1500, 2000, 2500, 2964, 3200, 4000, 4879, 5000, 5750, 6000,
            7500, 8200, 9500, 10000, 12000, 15000, 20000, 30000, 42000, 50000, 85000
    };

    private RealisticBankAmount() {}

    static BigDecimal generate(Random random, int minRupees, int maxRupees) {
        int min = Math.max(1, minRupees);
        int max = Math.max(min, maxRupees);
        int rupees = pickRupees(random, min, max);
        int cents = pickCents(random);
        BigDecimal result = toAmount(rupees, cents);
        BigDecimal minTotal = new BigDecimal(min).setScale(2, RoundingMode.HALF_UP);
        BigDecimal maxTotal = new BigDecimal(max).setScale(2, RoundingMode.HALF_UP);
        if (result.compareTo(maxTotal) > 0) {
            result = snapMaxToRealistic(maxTotal);
        }
        if (result.compareTo(minTotal) < 0) {
            result = minTotal;
        }
        return result;
    }

    /** Whole rupees only (always xxxx.00) — used for Kotak withdrawal/deposit amounts. */
    static BigDecimal generateWholeRupees(Random random, int minRupees, int maxRupees) {
        int min = Math.max(1, minRupees);
        int max = Math.max(min, maxRupees);
        int rupees = pickRupees(random, min, max);
        BigDecimal result = toAmount(rupees, 0);
        BigDecimal minTotal = new BigDecimal(min).setScale(2, RoundingMode.HALF_UP);
        BigDecimal maxTotal = new BigDecimal(max).setScale(2, RoundingMode.HALF_UP);
        if (result.compareTo(maxTotal) > 0) {
            result = maxTotal;
        }
        if (result.compareTo(minTotal) < 0) {
            result = minTotal;
        }
        return result;
    }

    /**
     * Kotak-oriented whole-rupee amounts: fewer round-tier picks and light de-rounding
     * so statements avoid repeated 20000 / 30000 / 42000 patterns.
     */
    static BigDecimal generateWholeRupeesNatural(Random random, int minRupees, int maxRupees) {
        int min = Math.max(1, minRupees);
        int max = Math.max(min, maxRupees);
        int rupees = pickRupeesNatural(random, min, max);
        BigDecimal result = toAmount(rupees, 0);
        BigDecimal minTotal = new BigDecimal(min).setScale(2, RoundingMode.HALF_UP);
        BigDecimal maxTotal = new BigDecimal(max).setScale(2, RoundingMode.HALF_UP);
        if (result.compareTo(maxTotal) > 0) {
            result = maxTotal;
        }
        if (result.compareTo(minTotal) < 0) {
            result = minTotal;
        }
        return result;
    }

    static BigDecimal wholeRupees(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.setScale(0, RoundingMode.DOWN).setScale(2, RoundingMode.UNNECESSARY);
    }

    /** Largest whole-rupee amount not exceeding {@code maxInclusive}. */
    static BigDecimal generateWholeRupeesUpTo(Random random, BigDecimal maxInclusive) {
        BigDecimal max = wholeRupees(maxInclusive);
        if (max.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return generateWholeRupees(random, 1, max.intValue());
    }

    /** Largest natural whole-rupee amount not exceeding {@code maxInclusive} (Kotak). */
    static BigDecimal generateWholeRupeesNaturalUpTo(Random random, BigDecimal maxInclusive) {
        BigDecimal max = wholeRupees(maxInclusive);
        if (max.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return generateWholeRupeesNatural(random, 1, max.intValue());
    }

    /** Largest realistic amount not exceeding {@code maxInclusive}. */
    static BigDecimal generateUpTo(Random random, BigDecimal maxInclusive) {
        BigDecimal max = maxInclusive.setScale(2, RoundingMode.HALF_UP);
        if (max.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        int maxRupees = max.intValue();
        if (maxRupees < 1) {
            int maxCents = max.multiply(BigDecimal.valueOf(100)).intValue();
            int cents = CENT_COMMON[random.nextInt(CENT_COMMON.length)];
            if (cents > maxCents) {
                cents = Math.max(0, maxCents);
            }
            return toAmount(0, cents);
        }

        for (int attempt = 0; attempt < 40; attempt++) {
            BigDecimal candidate = generate(random, 1, maxRupees);
            if (candidate.compareTo(max) <= 0) {
                return candidate;
            }
        }
        return snapMaxToRealistic(max);
    }

    static BigDecimal capToBalance(Random random, BigDecimal balance, BigDecimal requested) {
        balance = balance.setScale(2, RoundingMode.HALF_UP);
        requested = requested.setScale(2, RoundingMode.HALF_UP);
        if (requested.compareTo(balance) <= 0 && requested.compareTo(BigDecimal.ZERO) > 0 && isRealistic(requested)) {
            return requested;
        }
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return generateUpTo(random, balance);
    }

    static boolean isRealistic(BigDecimal amount) {
        amount = amount.setScale(2, RoundingMode.HALF_UP);
        int cents = amount.remainder(BigDecimal.ONE).multiply(BigDecimal.valueOf(100)).intValue();
        return isRealisticCent(cents);
    }

    private static BigDecimal snapMaxToRealistic(BigDecimal max) {
        int rupees = max.intValue();
        int cents = max.remainder(BigDecimal.ONE).multiply(BigDecimal.valueOf(100)).intValue();
        if (isRealisticCent(cents)) {
            return max;
        }
        int snappedCents = snapCent(cents);
        if (snappedCents > cents && rupees > 0) {
            rupees--;
            snappedCents = 90;
        }
        return toAmount(rupees, snappedCents);
    }

    private static int snapCent(int cents) {
        int best = 0;
        int bestDiff = 100;
        for (int c : CENT_COMMON) {
            int diff = Math.abs(c - cents);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = c;
            }
        }
        for (int c : CENT_SPECIAL) {
            int diff = Math.abs(c - cents);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = c;
            }
        }
        return best;
    }

    private static boolean isRealisticCent(int cents) {
        if (cents == 0) {
            return true;
        }
        for (int c : CENT_COMMON) {
            if (c == cents) {
                return true;
            }
        }
        for (int c : CENT_SPECIAL) {
            if (c == cents) {
                return true;
            }
        }
        return false;
    }

    private static int pickRupees(Random random, int min, int max) {
        if (random.nextInt(100) < 45) {
            int nice = pickNiceInRange(random, min, max);
            if (nice > 0) {
                return nice;
            }
        }

        int tier = pickTier(random, min, max);
        int tierMin = Math.max(min, tierLow(tier));
        int tierMax = Math.min(max, tierHigh(tier));
        if (tierMin > tierMax) {
            tierMin = min;
            tierMax = max;
        }
        return tierMin + random.nextInt(tierMax - tierMin + 1);
    }

    private static int pickRupeesNatural(Random random, int min, int max) {
        if (random.nextInt(100) < 12) {
            int nice = pickNiceInRange(random, min, max);
            if (nice > 0) {
                return deRoundSuspiciousAmount(random, nice, min, max);
            }
        }

        int tier = pickTier(random, min, max);
        int tierMin = Math.max(min, tierLow(tier));
        int tierMax = Math.min(max, tierHigh(tier));
        if (tierMin > tierMax) {
            tierMin = min;
            tierMax = max;
        }
        int value = tierMin + random.nextInt(tierMax - tierMin + 1);
        return deRoundSuspiciousAmount(random, value, min, max);
    }

    private static int deRoundSuspiciousAmount(Random random, int rupees, int min, int max) {
        if (rupees >= 10000 && rupees % 5000 == 0) {
            int[] offsets = {137, 249, 318, 421, 563, 687, 812, 924};
            int adjusted = rupees + offsets[random.nextInt(offsets.length)];
            if (adjusted <= max) {
                return adjusted;
            }
            adjusted = rupees - offsets[random.nextInt(offsets.length)];
            if (adjusted >= min) {
                return adjusted;
            }
        }
        if (rupees >= 1000 && rupees % 1000 == 0 && random.nextInt(100) < 70) {
            int adjusted = rupees + 100 + random.nextInt(900);
            if (adjusted <= max) {
                return adjusted;
            }
        }
        return rupees;
    }

    private static int pickNiceInRange(Random random, int min, int max) {
        for (int attempt = 0; attempt < 25; attempt++) {
            int candidate = NICE_RUPEES[random.nextInt(NICE_RUPEES.length)];
            if (candidate >= min && candidate <= max) {
                return candidate;
            }
        }
        return -1;
    }

    private static int pickTier(Random random, int min, int max) {
        int start = random.nextInt(4);
        for (int i = 0; i < 4; i++) {
            int tier = 2 + ((start + i) % 4);
            if (tierHigh(tier) >= min && tierLow(tier) <= max) {
                return tier;
            }
        }
        if (max < 100) {
            return 2;
        }
        if (max < 1000) {
            return 3;
        }
        if (max < 10000) {
            return 4;
        }
        return 5;
    }

    private static int tierLow(int tier) {
        return switch (tier) {
            case 2 -> 10;
            case 3 -> 100;
            case 4 -> 1000;
            default -> 10000;
        };
    }

    private static int tierHigh(int tier) {
        return switch (tier) {
            case 2 -> 99;
            case 3 -> 999;
            case 4 -> 9999;
            default -> 99999;
        };
    }

    private static int pickCents(Random random) {
        int roll = random.nextInt(100);
        if (roll < 83) {
            return 0;
        }
        if (roll < 96) {
            return CENT_COMMON[random.nextInt(CENT_COMMON.length)];
        }
        return CENT_SPECIAL[random.nextInt(CENT_SPECIAL.length)];
    }

    private static BigDecimal toAmount(int rupees, int cents) {
        return new BigDecimal(String.format("%d.%02d", rupees, cents));
    }
}
