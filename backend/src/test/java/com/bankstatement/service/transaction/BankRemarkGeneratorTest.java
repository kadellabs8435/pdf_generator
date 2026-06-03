package com.bankstatement.service.transaction;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class BankRemarkGeneratorTest {

    private static final Pattern UPI_DEBIT = Pattern.compile("^UPI/\\d{12}/[A-Z0-9 ]+/(BKID|KKBK|SBIN)$");
    private static final Pattern ATM = Pattern.compile("^(ATM WDL|ATM CASH WDL|CASH WDL ATM)/.+");

    @Test
    void configuredSalaryUsesCompanyNameAndRotatesPatterns() {
        Random random = new Random(42);
        Set<String> patterns = new HashSet<>();
        for (int i = 0; i < 30; i++) {
            BankRemarkGenerator.SalaryRemark remark = BankRemarkGenerator.configuredSalary(
                    "Infosys Ltd", LocalDate.of(2026, 5, 5), random);
            assertTrue(remark.narration().contains("INFOSYS LTD"), remark.narration());
            assertFalse(remark.narration().toLowerCase().contains("test"));
            assertFalse(remark.narration().toLowerCase().contains("demo"));
            patterns.add(remark.narration());
        }
        assertTrue(patterns.size() >= 4, "Expected rotated salary patterns");
    }

    @Test
    void bankSpecificUpiDebitUsesCorrectCode() {
        Random random = new Random(7);
        assertTrue(UPI_DEBIT.matcher(BankRemarkGenerator.upiDebit(random, "BOI")).matches());
        assertTrue(BankRemarkGenerator.upiDebit(random, "KOTAK").endsWith("/KKBK"));
        assertTrue(BankRemarkGenerator.upiDebit(random, "SBI").endsWith("/SBIN"));
    }

    @Test
    void atmEmiInterestAndUtilityLookRealistic() {
        Random random = new Random(99);
        assertTrue(ATM.matcher(BankRemarkGenerator.atmWithdrawal(random, "BOI")).matches());
        for (int i = 0; i < 10; i++) {
            String emi = BankRemarkGenerator.emiDebit(random);
            assertTrue(emi.startsWith("ECS/") || emi.startsWith("ACH DEBIT/") || emi.startsWith("EMI PAYMENT/"), emi);
        }
        assertFalse(BankRemarkGenerator.interestCredit(random).isBlank());
        assertFalse(BankRemarkGenerator.utilityDebit(random).isBlank());
        assertFalse(BankRemarkGenerator.smallIncomingCredit(random, "SBI").isBlank());
    }

    @Test
    void kotakConfiguredSalaryUsesThreePatternsOnly() {
        Random random = new Random(42);
        Set<String> prefixes = new HashSet<>();
        for (int i = 0; i < 30; i++) {
            BankRemarkGenerator.SalaryRemark remark =
                    BankRemarkGenerator.kotakConfiguredSalary("ACME CORP", random);
            assertTrue(remark.narration().contains("ACME CORP"));
            assertTrue(remark.narration().startsWith("SALARY CREDIT /")
                    || remark.narration().startsWith("PAYROLL /")
                    || remark.narration().startsWith("NEFT SALARY /"));
            prefixes.add(remark.narration().split("ACME")[0].trim());
        }
        assertTrue(prefixes.size() >= 2);
    }
}
