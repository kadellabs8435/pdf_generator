package com.bankstatement.service.transaction;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class HighValueAmountRulesTest {

    @Test
    void clusteredCreditStaysInWeightedBands() {
        Random random = new Random(42);
        for (int i = 0; i < 200; i++) {
            BigDecimal credit = HighValueAmountRules.clusteredCredit(random);
            int rupees = credit.intValue();
            assertTrue(rupees >= 15_000 && rupees <= 90_000, "Credit out of band: " + credit);
            assertEquals(0, credit.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO),
                    "Credits should be whole rupees: " + credit);
        }
    }

    @Test
    void clusteredDebitInFifteenToThirtyThousand() {
        Random random = new Random(7);
        for (int i = 0; i < 100; i++) {
            BigDecimal debit = HighValueAmountRules.clusteredDebit(random);
            int rupees = debit.intValue();
            assertTrue(rupees >= 15_000 && rupees <= 30_000, "Debit out of band: " + debit);
        }
    }

    @Test
    void interestUsesFixedDecimals() {
        for (int i = 0; i < 20; i++) {
            BigDecimal interest = HighValueAmountRules.interestCredit(new Random(i));
            assertTrue(interest.compareTo(new BigDecimal("100")) > 0);
            assertTrue(interest.compareTo(new BigDecimal("400")) < 0);
            assertNotEquals(0, interest.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO));
        }
    }
}
