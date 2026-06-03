package com.bankstatement.service.transaction;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class RealisticBankAmountTest {

    private static final Pattern TWO_DECIMALS = Pattern.compile("^\\d+\\.\\d{2}$");

    @Test
    void alwaysExactlyTwoDecimalPlaces() {
        Random r = new Random(42);
        for (int i = 0; i < 200; i++) {
            BigDecimal amt = RealisticBankAmount.generate(r, 10, 50000);
            assertEquals(2, amt.scale());
            assertTrue(TWO_DECIMALS.matcher(amt.toPlainString()).matches(), amt.toPlainString());
            assertTrue(RealisticBankAmount.isRealistic(amt), amt.toPlainString());
        }
    }

    @Test
    void noFakeFloatingPrecision() {
        Random r = new Random(99);
        for (int i = 0; i < 300; i++) {
            BigDecimal amt = RealisticBankAmount.generate(r, 5, 99999);
            String plain = amt.toPlainString();
            assertFalse(plain.contains("333"));
            assertFalse(plain.matches(".*\\.\\d{3,}.*"));
            int cents = amt.remainder(BigDecimal.ONE).multiply(BigDecimal.valueOf(100)).intValue();
            assertTrue(cents == 0 || cents == 50 || cents == 25 || cents == 75 || cents == 90
                    || isSpecialCent(cents), "unexpected cents: " + cents + " in " + plain);
        }
    }

    @Test
    void generateUpToRespectsMax() {
        Random r = new Random(7);
        BigDecimal max = new BigDecimal("2868.00");
        for (int i = 0; i < 50; i++) {
            BigDecimal amt = RealisticBankAmount.generateUpTo(r, max);
            assertTrue(amt.compareTo(max) <= 0);
            assertTrue(RealisticBankAmount.isRealistic(amt));
        }
    }

    @Test
    void producesVarietyOfSizes() {
        Random r = new Random(123);
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            seen.add(RealisticBankAmount.generate(r, 10, 85000).toPlainString());
        }
        assertTrue(seen.size() > 40);
    }

    private boolean isSpecialCent(int cents) {
        int[] special = {7, 35, 12, 57, 3, 15, 20, 99, 5, 10, 80, 60};
        for (int c : special) {
            if (c == cents) return true;
        }
        return false;
    }
}
