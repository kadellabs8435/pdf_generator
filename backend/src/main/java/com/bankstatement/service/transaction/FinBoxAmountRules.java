package com.bankstatement.service.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

/** Deterministic amount tiers for salaried-account realism (FinBox-friendly). */
final class FinBoxAmountRules {

    static final BigDecimal MAX_PREFERRED_BALANCE = GeneratedStatementRules.MAX_RUNNING_BALANCE;

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    enum Merchant {
        AIRTEL, JIO, SWIGGY, ZOMATO, DMART, IRCTC, FRIEND, ATM, UTILITY, EMI, GENERIC
    }

    private FinBoxAmountRules() {}

    /** Non-salary activity credit: ₹17k–₹45k. */
    static BigDecimal activityCredit(Random random) {
        return GeneratedStatementRules.activityCredit(random);
    }

    /** Non-salary activity debit: ₹16k–₹28k. */
    static BigDecimal activityDebit(Random random, Merchant merchant) {
        return GeneratedStatementRules.activityDebit(random);
    }

    static Merchant randomDebitMerchant(Random random) {
        return switch (random.nextInt(100)) {
            case 0, 1 -> Merchant.AIRTEL;
            case 2, 3 -> Merchant.JIO;
            case 4, 5, 6 -> Merchant.SWIGGY;
            case 7, 8 -> Merchant.ZOMATO;
            case 9, 10 -> Merchant.DMART;
            case 11 -> Merchant.IRCTC;
            case 12, 13 -> Merchant.FRIEND;
            case 14 -> Merchant.ATM;
            case 15 -> Merchant.UTILITY;
            case 16 -> Merchant.EMI;
            default -> Merchant.GENERIC;
        };
    }

    static Merchant merchantFromParty(String party) {
        if (party == null) {
            return Merchant.GENERIC;
        }
        String upper = party.toUpperCase();
        if (upper.contains("AIRTEL")) {
            return Merchant.AIRTEL;
        }
        if (upper.contains("JIO")) {
            return Merchant.JIO;
        }
        if (upper.contains("SWIGGY")) {
            return Merchant.SWIGGY;
        }
        if (upper.contains("ZOMATO")) {
            return Merchant.ZOMATO;
        }
        if (upper.contains("DMART")) {
            return Merchant.DMART;
        }
        if (upper.contains("IRCTC")) {
            return Merchant.IRCTC;
        }
        return Merchant.FRIEND;
    }

    static BigDecimal capDebitToBalance(BigDecimal balance, BigDecimal amount, boolean kotak) {
        balance = scale(balance);
        amount = scale(amount);
        BigDecimal max = scale(balance.subtract(
                kotak ? ZERO : GeneratedStatementRules.MIN_RUNNING_BALANCE));
        if (max.compareTo(ZERO) <= 0) {
            return ZERO;
        }
        if (amount.compareTo(max) > 0) {
            amount = RealisticBankAmount.generateUpTo(new Random(amount.hashCode()), max);
        }
        int maxRupees = Math.min(FinBoxAmountRules.DEBIT_RARE_MAX, max.intValue());
        if (amount.intValue() > maxRupees) {
            amount = RealisticBankAmount.generate(randomFrom(amount), 50, maxRupees);
        }
        return amount.compareTo(max) <= 0 ? amount : max;
    }

    static final int DEBIT_RARE_MAX = GeneratedStatementRules.ACTIVITY_DEBIT_MAX;

    private static BigDecimal pickGenericDebit(Random random) {
        return GeneratedStatementRules.activityDebit(random);
    }

    static BigDecimal interestCredit(Random random) {
        return RealisticBankAmount.generate(random, 15, 120);
    }

    static BigDecimal scale(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static Random randomFrom(BigDecimal seed) {
        return new Random(seed.longValue() ^ 0x5A17L);
    }
}
