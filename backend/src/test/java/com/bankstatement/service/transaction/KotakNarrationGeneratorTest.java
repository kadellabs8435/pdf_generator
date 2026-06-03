package com.bankstatement.service.transaction;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class KotakNarrationGeneratorTest {

    private static final Pattern UPI_DEBIT = Pattern.compile("^UPI/[A-Z ]+/\\d{12}/UPI$");
    private static final Pattern UPI_CREDIT = Pattern.compile("^UPI CREDIT/[A-Z ]+$");
    private static final Pattern UPI_CR = Pattern.compile("^UPI-CR/[A-Z ]+/\\d{12}$");
    private static final Pattern UPI_REV = Pattern.compile("^UPI/REV/[A-Z ]+/\\d{12}$");
    private static final Pattern IMPS = Pattern.compile("^IMPS/[A-Z ]+/Transfer$");
    private static final Pattern NEFT = Pattern.compile("^NEFT/[A-Z ]+/Transfer$");
    private static final Pattern INT_CREDIT = Pattern.compile("^INT\\. CREDIT \\d+\\.\\d{2}$");

    private KotakNarrationGenerator generator(long seed) {
        Random random = new Random(seed);
        return new KotakNarrationGenerator(random, new KotakEcosystemContext(random, "5251182222"));
    }

    @Test
    void upiDebitUsesFinBoxFormat() {
        KotakNarrationGenerator generator = generator(42);
        for (int i = 0; i < 30; i++) {
            KotakNarrationGenerator.KotakTxnEntry debit =
                    generator.forChannel(KotakChannel.UPI_DEBIT, ActivityFlowPlanner.Direction.DEBIT);
            assertTrue(UPI_DEBIT.matcher(debit.narration()).matches(), debit.narration());
            assertTrue(KotakNarrationGenerator.isDebitNarration(debit.narration()), debit.narration());
        }
    }

    @Test
    void upiCreditVariantsMatchAcceptedFormats() {
        KotakNarrationGenerator generator = generator(7);
        assertTrue(UPI_CREDIT.matcher(generator.forChannel(KotakChannel.UPI_CREDIT).narration()).matches());
        assertTrue(UPI_CR.matcher(generator.forChannel(KotakChannel.UPI_CR).narration()).matches());
        assertTrue(UPI_REV.matcher(generator.forChannel(KotakChannel.UPI_REV).narration()).matches());
    }

    @Test
    void impsAndNeftUseTransferFormat() {
        KotakNarrationGenerator generator = generator(11);
        assertTrue(IMPS.matcher(generator.forChannel(KotakChannel.IMPS).narration()).matches());
        assertTrue(NEFT.matcher(generator.forChannel(KotakChannel.NEFT).narration()).matches());
    }

    @Test
    void interestUsesIntCreditFormat() {
        KotakNarrationGenerator generator = generator(3);
        var entry = generator.forChannel(KotakChannel.INT_CREDIT);
        assertTrue(INT_CREDIT.matcher(entry.narration()).matches(), entry.narration());
        assertNotNull(entry.fixedAmount());
        assertTrue(entry.narration().contains(entry.fixedAmount().toPlainString()));
    }

    @Test
    void usesDiverseNamePool() {
        Random random = new Random(99);
        KotakEcosystemContext ecosystem = new KotakEcosystemContext(random, "5251182222");
        KotakNarrationGenerator generator = new KotakNarrationGenerator(random, ecosystem);
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 60; i++) {
            String narration = generator.forChannel(KotakChannel.UPI_DEBIT).narration();
            seen.add(narration.split("/")[1]);
        }
        assertTrue(seen.size() >= 15, "Expected diverse names, saw: " + seen.size());
    }
}
