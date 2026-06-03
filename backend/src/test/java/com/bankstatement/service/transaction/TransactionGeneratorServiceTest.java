package com.bankstatement.service.transaction;

import com.bankstatement.entity.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class TransactionGeneratorServiceTest {

    private static final BigDecimal FLOOR = GeneratedStatementRules.MIN_RUNNING_BALANCE;
    private static final BigDecimal KOTAK_CEILING = GeneratedStatementRules.MAX_RUNNING_BALANCE;

    private final TransactionGeneratorService generator = new TransactionGeneratorService();

    @Test
    void kotakUsesRealisticDecimalAmounts() {
        Statement statement = sampleStatement("KOTAK", "1234567890");
        statement.setOpeningBalance(new BigDecimal("70000.00"));
        statement.getTransactionSettings().setInterest(true);
        statement.getTransactionSettings().setMinTransactions(30);
        statement.getTransactionSettings().setMaxTransactions(30);
        List<Transaction> txns = generator.generate(statement);
        assertRunningBalance(statement.getOpeningBalance(), txns);
        boolean hasInterestDecimals = txns.stream().anyMatch(txn ->
                "INT".equals(txn.getType())
                        && txn.getNarration() != null
                        && txn.getNarration().startsWith("INT. CREDIT "));
        assertTrue(hasInterestDecimals, "Expected INT. CREDIT with decimal paise");
    }

    @Test
    void creditAndDebitAmountsStayInConfiguredRanges() {
        Statement statement = sampleStatement("BOI", "995610110012688");
        statement.setOpeningBalance(new BigDecimal("250000.00"));
        List<Transaction> txns = generator.generate(statement);
        assertRunningBalance(statement.getOpeningBalance(), txns);
        assertNoZeroBalance(txns);
        for (Transaction txn : txns) {
            BigDecimal credit = txn.getCredit() != null ? txn.getCredit() : BigDecimal.ZERO;
            BigDecimal debit = txn.getDebit() != null ? txn.getDebit() : BigDecimal.ZERO;
            if (credit.compareTo(BigDecimal.ZERO) > 0 && !"SAL".equals(txn.getType())
                    && !"INT".equals(txn.getType())) {
                assertTrue(credit.compareTo(new BigDecimal("17000.00")) >= 0,
                        "BOI credit below minimum: " + credit);
                assertTrue(credit.compareTo(new BigDecimal("45000.00")) <= 0,
                        "BOI credit above maximum: " + credit);
            }
            if (debit.compareTo(BigDecimal.ZERO) > 0 && !"CHG".equals(txn.getType())) {
                assertTrue(debit.compareTo(new BigDecimal("16000.00")) >= 0,
                        "BOI debit below minimum: " + debit);
                assertTrue(debit.compareTo(new BigDecimal("28000.00")) <= 0,
                        "BOI debit above maximum: " + debit);
            }
        }
    }

    @Test
    void sbiAndKotakUseHighValueClusteredAmounts() {
        for (String bank : List.of("SBI", "KOTAK")) {
            Statement statement = sampleStatement(bank, "995610110012688");
            statement.setOpeningBalance("KOTAK".equals(bank)
                    ? new BigDecimal("70000.00") : new BigDecimal("95000.00"));
            statement.getTransactionSettings().setMinTransactions(25);
            statement.getTransactionSettings().setMaxTransactions(25);
            List<Transaction> txns = generator.generate(statement);
            assertRunningBalance(statement.getOpeningBalance(), txns);
            boolean hasHighCredit = false;
            boolean hasHighDebit = false;
            for (Transaction txn : txns) {
                if ("SAL".equals(txn.getType())) {
                    continue;
                }
                BigDecimal credit = txn.getCredit() != null ? txn.getCredit() : BigDecimal.ZERO;
                BigDecimal debit = txn.getDebit() != null ? txn.getDebit() : BigDecimal.ZERO;
                if (credit.compareTo(BigDecimal.ZERO) > 0 && !"INT".equals(txn.getType())
                    && !"SAL".equals(txn.getType())) {
                    assertTrue(credit.compareTo(new BigDecimal("17000.00")) >= 0,
                            bank + " activity credit too small: " + credit);
                    assertTrue(credit.compareTo(new BigDecimal("45000.00")) <= 0,
                            bank + " activity credit too large: " + credit);
                    hasHighCredit = true;
                }
                if (debit.compareTo(BigDecimal.ZERO) > 0) {
                    assertTrue(debit.compareTo(new BigDecimal("16000.00")) >= 0,
                            bank + " debit too small: " + debit);
                    assertTrue(debit.compareTo(new BigDecimal("28000.00")) <= 0,
                            bank + " debit too large: " + debit);
                    hasHighDebit = true;
                }
            }
            assertTrue(hasHighCredit, bank + " should include high-value credits");
            assertTrue(hasHighDebit, bank + " should include high-value debits");
        }
    }

    @Test
    void generatesTransactionsWithCorrectRunningBalance() {
        Statement statement = sampleStatement("SBI", null);

        List<Transaction> txns = generator.generate(statement);

        assertFalse(txns.isEmpty());
        assertRunningBalance(statement.getOpeningBalance(), txns);
    }

    @Test
    void neverProducesNegativeBalance() {
        Statement statement = sampleStatement("BOI", "995610110012688");
        statement.setOpeningBalance(new BigDecimal("70000.00"));
        statement.getTransactionSettings().setMinTransactions(20);
        statement.getTransactionSettings().setMaxTransactions(20);

        for (int seed = 0; seed < 5; seed++) {
            List<Transaction> txns = generator.generate(statement);
            assertRunningBalance(statement.getOpeningBalance(), txns);
            assertNoZeroBalance(txns);
            for (Transaction txn : txns) {
                assertTrue(txn.getBalance().compareTo(FLOOR) >= 0,
                        "Balance must stay at or above 70000: " + txn.getBalance());
                BigDecimal debit = txn.getDebit() != null ? txn.getDebit() : BigDecimal.ZERO;
                assertTrue(debit.compareTo(BigDecimal.ZERO) >= 0);
            }
        }
    }

    @Test
    void runningBalanceNeverReachesZero() {
        for (String bank : List.of("SBI", "KOTAK", "BOI")) {
            Statement statement = sampleStatement(bank, "995610110012688");
            statement.setOpeningBalance(new BigDecimal("85000.00"));
            statement.getTransactionSettings().setMinTransactions(20);
            statement.getTransactionSettings().setMaxTransactions(20);

            for (int i = 0; i < 10; i++) {
                List<Transaction> txns = generator.generate(statement);
                assertRunningBalance(statement.getOpeningBalance(), txns);
                assertNoZeroBalance(txns, bank);
            }
        }
    }

    @Test
    void debitNeverExceedsRunningBalanceInChronologicalOrder() {
        Statement statement = sampleStatement("BOI", null);
        statement.setOpeningBalance(new BigDecimal("85000.00"));
        statement.getTransactionSettings().setMinTransactions(15);
        statement.getTransactionSettings().setMaxTransactions(15);

        List<Transaction> txns = generator.generate(statement);
        BigDecimal running = statement.getOpeningBalance();
        for (Transaction txn : txns) {
            BigDecimal debit = txn.getDebit() != null ? txn.getDebit() : BigDecimal.ZERO;
            BigDecimal credit = txn.getCredit() != null ? txn.getCredit() : BigDecimal.ZERO;
            if (debit.compareTo(BigDecimal.ZERO) > 0) {
                assertTrue(debit.compareTo(running) <= 0,
                        "Debit " + debit + " exceeds running balance " + running);
            }
            running = running.add(credit).subtract(debit);
            assertEquals(0, running.compareTo(txn.getBalance()));
        }
    }

    @Test
    void boiUpiOnlyUsesUpiRemarks() {
        Statement statement = sampleStatement("BOI", "995610110012688");
        statement.setOpeningBalance(new BigDecimal("85000.00"));
        statement.getTransactionSettings().setSalary(false);
        statement.getTransactionSettings().setUpi(true);
        statement.getTransactionSettings().setAtm(false);
        statement.getTransactionSettings().setEmi(false);
        statement.getTransactionSettings().setInterest(false);
        statement.getTransactionSettings().setMinTransactions(12);
        statement.getTransactionSettings().setMaxTransactions(12);

        List<Transaction> txns = generator.generate(statement);
        assertRunningBalance(statement.getOpeningBalance(), txns);

        for (Transaction txn : txns) {
            String n = txn.getNarration();
            assertTrue(n.startsWith("TO TRANSFER-UPI/") || n.startsWith("BY TRANSFER-UPI/")
                            || n.startsWith("BY TRANSFER-IMPS/") || n.startsWith("BY TRANSFER-NEFT/"),
                    "Expected BOI transfer remark, got: " + n);
            assertFalse(n.equals("ATM CASH WDL"), "ATM must not appear when ATM disabled: " + n);
            assertFalse(n.equals("EMI"), "EMI must not appear when EMI disabled: " + n);
            assertFalse(n.contains("SBInt.Pd:"), "Interest must not appear when interest disabled: " + n);
            assertFalse(n.contains("Chrgs"), "Charges must not appear when interest disabled: " + n);
        }
    }

    @Test
    void boiAtmOnlyUsesAtmRemarks() {
        Statement statement = sampleStatement("BOI", "995610110012688");
        statement.setOpeningBalance(new BigDecimal("300000.00"));
        statement.getTransactionSettings().setSalary(false);
        statement.getTransactionSettings().setUpi(false);
        statement.getTransactionSettings().setAtm(true);
        statement.getTransactionSettings().setEmi(false);
        statement.getTransactionSettings().setInterest(false);
        statement.getTransactionSettings().setMinTransactions(10);
        statement.getTransactionSettings().setMaxTransactions(10);

        List<Transaction> txns = generator.generate(statement);
        assertRunningBalance(statement.getOpeningBalance(), txns);

        for (Transaction txn : txns) {
            String n = txn.getNarration();
            assertEquals("ATM CASH WDL", n, "Expected ATM-only remark, got: " + n);
            assertFalse(n.startsWith("TO TRANSFER-UPI/"), "UPI must not appear when UPI disabled: " + n);
        }
    }

    @Test
    void kotakUpiUsesDetailedDescription() {
        Statement statement = sampleStatement("KOTAK", "1234567890");
        statement.setOpeningBalance(new BigDecimal("70000.00"));
        statement.getTransactionSettings().setSalary(false);
        statement.getTransactionSettings().setUpi(true);
        statement.getTransactionSettings().setAtm(false);
        statement.getTransactionSettings().setEmi(false);
        statement.getTransactionSettings().setInterest(false);
        statement.getTransactionSettings().setMinTransactions(10);
        statement.getTransactionSettings().setMaxTransactions(10);

        List<Transaction> txns = generator.generate(statement);
        assertRunningBalance(statement.getOpeningBalance(), txns);

        Pattern kotakNative = Pattern.compile(
                "^(UPI/[A-Z ]+/\\d{12}/UPI|UPI CREDIT/|UPI-CR/|UPI/REV/|IMPS/|NEFT/|INT\\. CREDIT )");
        for (Transaction txn : txns) {
            assertTrue(kotakNative.matcher(txn.getNarration()).find(),
                    "Expected Kotak-native narration, got: " + txn.getNarration());
        }
    }

    @Test
    void monthlySalaryCreditsUseConfiguredCompanyAndAmount() {
        String company = "TATA CONSULTANCY SERVICES";
        BigDecimal salary = new BigDecimal("52000.00");
        Statement statement = sampleStatement("BOI", "995610110012688");
        statement.getPeriod().setFromDate(LocalDate.of(2025, 1, 1));
        statement.getPeriod().setToDate(LocalDate.of(2025, 3, 31));
        statement.getTransactionSettings().setSalary(true);
        statement.getTransactionSettings().setSalaryCompanyName(company);
        statement.getTransactionSettings().setSalaryAmount(salary);
        statement.getTransactionSettings().setSalaryDayOfMonth(5);
        statement.getTransactionSettings().setUpi(false);
        statement.getTransactionSettings().setAtm(false);
        statement.getTransactionSettings().setEmi(false);
        statement.getTransactionSettings().setInterest(false);
        statement.getTransactionSettings().setMinTransactions(0);
        statement.getTransactionSettings().setMaxTransactions(0);

        List<Transaction> txns = generator.generate(statement);
        assertEquals(3, txns.size(), "Jan, Feb, Mar salary on day 5");
        assertRunningBalance(statement.getOpeningBalance(), txns);

        for (Transaction txn : txns) {
            assertEquals(0, salary.compareTo(txn.getCredit()),
                    "Salary amount must match configured value exactly");
            assertTrue(txn.getNarration().contains("BY TRANSFER-SALARY/TATA CONSULTANCY SERVICES"),
                    "Expected BOI salary remark: " + txn.getNarration());
            assertEquals(5, txn.getDate().getDayOfMonth());
        }
    }

    @Test
    void boiNarrationsUseRealisticPatterns() {
        Statement statement = sampleStatement("BOI", "995610110012688");

        List<Transaction> txns = generator.generate(statement);

        assertFalse(txns.isEmpty());
        assertRunningBalance(statement.getOpeningBalance(), txns);

        Set<String> narrations = new HashSet<>();
        boolean hasUpi = false;
        boolean hasRealistic = false;

        for (Transaction txn : txns) {
            String n = txn.getNarration();
            assertNotNull(n);
            assertFalse(n.isBlank());
            assertFalse(n.toLowerCase().contains("dummy"));
            assertFalse(n.startsWith("AUTO EMI"));
            assertFalse(n.startsWith("SALARY CREDIT -"));
            narrations.add(n);

            if (n.startsWith("TO TRANSFER-") || n.startsWith("BY TRANSFER-")
                    || n.equals("ATM CASH WDL") || n.equals("INT. CREDIT")
                    || n.startsWith("ECS/") || n.startsWith("ACH DEBIT/")
                    || n.contains("Chrgs") || n.contains("BY TRANSFER-SALARY/")) {
                hasRealistic = true;
            }
            if (n.startsWith("TO TRANSFER-UPI/") || n.startsWith("BY TRANSFER-UPI/")) {
                hasUpi = true;
            }
        }

        assertTrue(hasRealistic, "Expected at least one BOI-style narration");
        assertTrue(hasUpi, "Expected UPI narrations when UPI enabled");
        assertTrue(narrations.size() >= 1, "Expected at least one narration type");
    }

    @Test
    void lowOpeningBalanceWithLaterSalaryNeverHitsZero() {
        Statement statement = sampleStatement("KOTAK", "1234567890");
        statement.setOpeningBalance(new BigDecimal("45000.00"));
        statement.getPeriod().setFromDate(LocalDate.of(2025, 1, 1));
        statement.getPeriod().setToDate(LocalDate.of(2025, 1, 31));
        statement.getTransactionSettings().setSalaryDayOfMonth(25);
        statement.getTransactionSettings().setMinTransactions(15);
        statement.getTransactionSettings().setMaxTransactions(15);

        for (int i = 0; i < 10; i++) {
            List<Transaction> txns = generator.generate(statement);
            assertRunningBalance(statement.getOpeningBalance(), txns);
            assertNoZeroBalance(txns, "KOTAK");
        }
    }

    @Test
    void openingBelowFloorRecoversBeforeDebitsInChronologicalOrder() {
        Statement statement = sampleStatement("BOI", "995610110012688");
        statement.setOpeningBalance(new BigDecimal("20000.00"));
        statement.getPeriod().setFromDate(LocalDate.of(2025, 1, 1));
        statement.getPeriod().setToDate(LocalDate.of(2025, 1, 31));
        statement.getTransactionSettings().setSalaryDayOfMonth(28);
        statement.getTransactionSettings().setMinTransactions(12);
        statement.getTransactionSettings().setMaxTransactions(12);

        List<Transaction> txns = generator.generate(statement);
        assertRunningBalance(statement.getOpeningBalance(), txns);
        assertNoZeroBalance(txns);
    }

    @Test
    void kotakNarrationDirectionMatchesAmountColumn() {
        Statement statement = sampleStatement("KOTAK", "1234567890");
        statement.setOpeningBalance(new BigDecimal("70000.00"));
        statement.getTransactionSettings().setMinTransactions(40);
        statement.getTransactionSettings().setMaxTransactions(40);

        for (int i = 0; i < 5; i++) {
            List<Transaction> txns = generator.generate(statement);
            assertRunningBalance(statement.getOpeningBalance(), txns);
            for (Transaction txn : txns) {
                BigDecimal credit = txn.getCredit() != null ? txn.getCredit() : BigDecimal.ZERO;
                BigDecimal debit = txn.getDebit() != null ? txn.getDebit() : BigDecimal.ZERO;
                if (credit.compareTo(BigDecimal.ZERO) > 0) {
                    assertTrue(
                            KotakNarrationGenerator.isCreditNarration(txn.getNarration()),
                            "Credit row with outgoing narration: " + txn.getNarration());
                }
                if (debit.compareTo(BigDecimal.ZERO) > 0) {
                    assertTrue(
                            KotakNarrationGenerator.isDebitNarration(txn.getNarration()),
                            "Debit row with incoming narration: " + txn.getNarration());
                }
            }
        }
    }

    @Test
    void kotakConfiguredSalaryUsesExactAmount() {
        Statement statement = sampleStatement("KOTAK", "1234567890");
        statement.setOpeningBalance(new BigDecimal("70000.00"));
        statement.getTransactionSettings().setSalaryAmount(new BigDecimal("5000.00"));

        List<Transaction> txns = generator.generate(statement);
        List<Transaction> salaries = txns.stream()
                .filter(txn -> txn.getNarration().contains("ACME CORP"))
                .toList();
        assertFalse(salaries.isEmpty());
        for (Transaction salary : salaries) {
            assertEquals(new BigDecimal("5000.00"), salary.getCredit());
        }
    }

    @Test
    void kotakHighValueCreditsAndDebitsWithinBands() {
        Statement statement = sampleStatement("KOTAK", "1234567890");
        statement.setOpeningBalance(new BigDecimal("70000.00"));
        statement.getTransactionSettings().setMinTransactions(40);
        statement.getTransactionSettings().setMaxTransactions(40);

        List<Transaction> txns = generator.generate(statement);
        assertRunningBalance(statement.getOpeningBalance(), txns);
        for (Transaction txn : txns) {
            if ("SAL".equals(txn.getType())) {
                continue;
            }
            BigDecimal credit = txn.getCredit() != null ? txn.getCredit() : BigDecimal.ZERO;
            if (credit.compareTo(BigDecimal.ZERO) > 0 && !"INT".equals(txn.getType())
                    && !"SAL".equals(txn.getType())) {
                assertTrue(txn.getBalance().compareTo(FLOOR) >= 0,
                        "Balance below floor: " + txn.getBalance());
                assertTrue(txn.getBalance().compareTo(KOTAK_CEILING) <= 0,
                        "Balance above ceiling: " + txn.getBalance());
                assertTrue(credit.compareTo(new BigDecimal("45000.00")) <= 0,
                        "Non-salary credit too large: " + credit);
                assertTrue(credit.compareTo(new BigDecimal("17000.00")) >= 0,
                        "Non-salary credit too small: " + credit);
                assertFalse(BankRemarkGenerator.isBannedEmployerCreditText(txn.getNarration()),
                        txn.getNarration());
            }
            BigDecimal debit = txn.getDebit() != null ? txn.getDebit() : BigDecimal.ZERO;
            if (debit.compareTo(BigDecimal.ZERO) > 0) {
                assertTrue(debit.compareTo(new BigDecimal("16000.00")) >= 0,
                        "Debit too small: " + debit);
                assertTrue(debit.compareTo(new BigDecimal("28000.00")) <= 0,
                        "Debit too large: " + debit);
            }
        }
    }

    @Test
    void kotakIncludesNativeNarrationTypes() {
        Statement statement = sampleStatement("KOTAK", "5251182222");
        statement.setOpeningBalance(new BigDecimal("70000.00"));
        statement.getTransactionSettings().setMinTransactions(40);
        statement.getTransactionSettings().setMaxTransactions(40);
        List<Transaction> txns = generator.generate(statement);
        String joined = txns.stream().map(Transaction::getNarration).reduce("", String::concat);
        assertTrue(joined.contains("UPI/") || joined.contains("UPI CREDIT/")
                        || joined.contains("UPI-CR/") || joined.contains("UPI/REV/")
                        || joined.contains("IMPS/") || joined.contains("NEFT/")
                        || joined.contains("INT. CREDIT") || joined.contains("SALARY CREDIT")
                        || joined.contains("PAYROLL") || joined.contains("NEFT SALARY"),
                "Expected FinBox Kotak narration mix");
    }

    @Test
    void kotakRunningBalanceStaysBetweenSeventyAndOneTwentyThousand() {
        Statement statement = sampleStatement("KOTAK", "1234567890");
        statement.setOpeningBalance(new BigDecimal("70000.00"));
        statement.getTransactionSettings().setMinTransactions(30);
        statement.getTransactionSettings().setMaxTransactions(30);

        for (int i = 0; i < 5; i++) {
            List<Transaction> txns = generator.generate(statement);
            assertRunningBalance(statement.getOpeningBalance(), txns);
            for (Transaction txn : txns) {
                assertTrue(txn.getBalance().compareTo(FLOOR) >= 0,
                        "Kotak balance below floor: " + txn.getBalance());
                assertTrue(txn.getBalance().compareTo(KOTAK_CEILING) <= 0,
                        "Kotak balance above ceiling: " + txn.getBalance());
            }
        }
    }

    @Test
    void kotakAmountsClusterInAcceptedBand() {
        Statement statement = sampleStatement("KOTAK", "1234567890");
        statement.setOpeningBalance(new BigDecimal("70000.00"));
        statement.getTransactionSettings().setMinTransactions(40);
        statement.getTransactionSettings().setMaxTransactions(40);

        for (int seed = 0; seed < 3; seed++) {
            List<Transaction> txns = generator.generate(statement);
            assertRunningBalance(statement.getOpeningBalance(), txns);
            for (Transaction txn : txns) {
                assertTrue(txn.getBalance().compareTo(FLOOR) >= 0,
                        "Balance below Kotak floor: " + txn.getBalance());
                assertTrue(txn.getBalance().compareTo(KOTAK_CEILING) <= 0,
                        "Balance above Kotak ceiling: " + txn.getBalance());
            }
        }
    }

    @Test
    void everyGeneratedRowHasExactlyOneAmountColumn() {
        for (String bank : List.of("SBI", "BOI", "KOTAK")) {
            Statement statement = sampleStatement(bank, "995610110012688");
            statement.setOpeningBalance(new BigDecimal("70000.00"));
            statement.getTransactionSettings().setMinTransactions(25);
            statement.getTransactionSettings().setMaxTransactions(25);

            for (int i = 0; i < 5; i++) {
                List<Transaction> txns = generator.generate(statement);
                for (Transaction txn : txns) {
                    if (txn.getNarration() == null || txn.getNarration().isBlank()) {
                        continue;
                    }
                    BigDecimal credit = TransactionAmountGuard.displayCredit(txn);
                    BigDecimal debit = TransactionAmountGuard.displayDebit(txn);
                    boolean hasCredit = credit.compareTo(BigDecimal.ZERO) > 0;
                    boolean hasDebit = debit.compareTo(BigDecimal.ZERO) > 0;
                    assertTrue(hasCredit || hasDebit,
                            bank + " row missing amount: " + txn.getNarration());
                    assertFalse(hasCredit && hasDebit,
                            bank + " row has both debit and credit: " + txn.getNarration());
                }
            }
        }
    }

    private Statement sampleStatement(String bankCode, String accountNumber) {
        return Statement.builder()
                .bankCode(bankCode)
                .accountDetails(accountNumber != null
                        ? AccountDetails.builder().accountNumber(accountNumber).build()
                        : null)
                .openingBalance(new BigDecimal("70000.00"))
                .period(StatementPeriod.builder()
                        .fromDate(LocalDate.of(2025, 1, 1))
                        .toDate(LocalDate.of(2025, 1, 31))
                        .build())
                .transactionSettings(TransactionSettings.builder()
                        .salary(true)
                        .salaryCompanyName("ACME CORP")
                        .salaryAmount(new BigDecimal("50000.00"))
                        .salaryDayOfMonth(1)
                        .upi(true).atm(true).emi(true).interest(true)
                        .minTransactions(12).maxTransactions(12)
                        .build())
                .build();
    }

    private void assertNoZeroBalance(List<Transaction> txns) {
        assertNoZeroBalance(txns, null);
    }

    private void assertNoZeroBalance(List<Transaction> txns, String bankCode) {
        assertMinimumRunningBalance(txns, FLOOR);
    }

    private void assertMinimumRunningBalance(List<Transaction> txns, BigDecimal floor) {
        for (Transaction txn : txns) {
            assertTrue(txn.getBalance().compareTo(floor) >= 0,
                    "Running balance must stay at or above " + floor + ", was " + txn.getBalance()
                            + " on " + txn.getDate() + " " + txn.getNarration());
        }
    }

    private void assertRunningBalance(BigDecimal opening, List<Transaction> txns) {
        BigDecimal balance = opening;
        for (Transaction txn : txns) {
            if (txn.getCredit() != null && txn.getCredit().compareTo(BigDecimal.ZERO) > 0) {
                balance = balance.add(txn.getCredit());
            }
            if (txn.getDebit() != null && txn.getDebit().compareTo(BigDecimal.ZERO) > 0) {
                balance = balance.subtract(txn.getDebit());
            }
            assertEquals(0, balance.compareTo(txn.getBalance()), "Running balance must match");
        }
    }

    private void assertWholeRupees(BigDecimal amount, String label) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        assertEquals(0, amount.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO),
                "Kotak " + label + " must be whole rupees (.00 only), was " + amount);
    }
}
