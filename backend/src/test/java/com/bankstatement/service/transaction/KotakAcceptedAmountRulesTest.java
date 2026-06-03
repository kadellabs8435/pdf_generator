package com.bankstatement.service.transaction;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class KotakAcceptedAmountRulesTest {

    @Test
    void activityAmountStaysInAcceptedBand() {
        Random random = new Random(42);
        for (int i = 0; i < 200; i++) {
            BigDecimal amount = KotakAcceptedAmountRules.activityAmount(random);
            int rupees = amount.intValue();
            assertTrue(rupees >= 15_000 && rupees <= 45_000, "Out of band: " + amount);
            assertTrue(rupees < 50_000, "Should avoid 50k+: " + amount);
        }
    }

    @Test
    void interestUsesDecimalAmounts() {
        for (int i = 0; i < 20; i++) {
            BigDecimal interest = KotakAcceptedAmountRules.interestAmount(new Random(i));
            assertNotEquals(0, interest.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO));
            assertTrue(interest.toPlainString().startsWith("INT. CREDIT ".substring(0, 1))
                    || KotakAcceptedAmountRules.interestNarration(interest).startsWith("INT. CREDIT "));
        }
    }
}
