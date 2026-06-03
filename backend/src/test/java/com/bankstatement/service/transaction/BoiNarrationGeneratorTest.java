package com.bankstatement.service.transaction;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class BoiNarrationGeneratorTest {

    private static final Pattern UPI_DR = Pattern.compile("^TO TRANSFER-UPI/[A-Z ]+$");
    private static final Pattern UPI_CR = Pattern.compile("^BY TRANSFER-UPI/[A-Z ]+$");
    private static final Pattern NEFT = Pattern.compile("^BY TRANSFER-NEFT/[A-Z ]+$");
    private static final Pattern IMPS = Pattern.compile("^BY TRANSFER-IMPS/[A-Z ]+$");
    private static final Pattern SALARY = Pattern.compile("^BY TRANSFER-SALARY/.+$");

    @Test
    void upiDebitMatchesBoiPattern() {
        Random r = new Random(42);
        for (int i = 0; i < 20; i++) {
            assertTrue(UPI_DR.matcher(BoiNarrationGenerator.upiDebit(r).narration()).matches());
        }
    }

    @Test
    void upiCreditMatchesBoiPattern() {
        Random r = new Random(99);
        for (int i = 0; i < 20; i++) {
            String narration = BoiNarrationGenerator.upiCredit(r).narration();
            assertTrue(UPI_CR.matcher(narration).matches()
                            || IMPS.matcher(narration).matches()
                            || NEFT.matcher(narration).matches(),
                    narration);
        }
    }

    @Test
    void neftAndAtmMatchPatterns() {
        Random r = new Random(7);
        assertTrue(NEFT.matcher(BoiNarrationGenerator.neftCredit(r).narration()).matches());
        assertTrue(BoiNarrationGenerator.neftDebit(r).narration().startsWith("TO TRANSFER-NEFT/"));
        assertEquals("ATM CASH WDL", BoiNarrationGenerator.atmWithdrawal(r).narration());
    }

    @Test
    void uniqueAvoidsDuplicatesWithinBatch() {
        Random r = new Random(123);
        Set<String> used = new HashSet<>();
        Set<String> generated = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            String n = BoiNarrationGenerator.unique(() -> BoiNarrationGenerator.upiDebit(r).narration(), used, r);
            assertTrue(generated.add(n), "Should not repeat: " + n);
        }
    }

    @Test
    void configuredSalaryUsesCompanyFromForm() {
        Random r = new Random(5);
        BankRemarkGenerator.SalaryRemark salary =
                BoiNarrationGenerator.configuredSalary("Acme Technologies Pvt Ltd", r);
        assertTrue(SALARY.matcher(salary.narration()).matches(), salary.narration());
        assertTrue(salary.narration().contains("ACME TECHNOLOGIES PVT LTD"));
    }

    @Test
    void interestUsesIntCreditLabel() {
        Random r = new Random(1);
        assertEquals("INT. CREDIT", BoiNarrationGenerator.interestCredit(
                "995610110012688",
                LocalDate.of(2025, 12, 1),
                LocalDate.of(2026, 3, 31),
                r));
    }
}
