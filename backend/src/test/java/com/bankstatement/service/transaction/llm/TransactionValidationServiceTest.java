package com.bankstatement.service.transaction.llm;

import com.bankstatement.entity.AccountDetails;
import com.bankstatement.entity.Statement;
import com.bankstatement.entity.StatementPeriod;
import com.bankstatement.entity.Transaction;
import com.bankstatement.entity.TransactionSettings;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransactionValidationServiceTest {

    private final TransactionValidationService validationService = new TransactionValidationService();

    @Test
    void validatesSalaryCompanyAmountAndDayExactly() {
        Statement statement = sampleStatement("SBI");
        List<LlmTransactionItem> items = new ArrayList<>();
        items.add(salaryItem("2026-04-05", "NEFT CR TATA CONSULTANCY SERVICES LTD"));
        items.add(salaryItem("2026-05-05", "NEFT CR TATA CONSULTANCY SERVICES LTD"));
        items.add(upiDebit("2026-04-10", 1800));
        items.add(upiDebit("2026-04-15", 2200));
        items.add(upiDebit("2026-04-20", 1900));
        items.add(upiDebit("2026-04-25", 2100));
        items.add(upiDebit("2026-04-28", 1700));
        items.add(upiDebit("2026-05-02", 2000));
        items.add(upiDebit("2026-05-08", 1850));
        items.add(upiDebit("2026-05-12", 1950));

        List<Transaction> txns = validationService.validateAndConvert(statement, items);

        assertEquals(10, txns.size());
        Transaction salary = txns.stream().filter(t -> "SAL".equals(t.getType())).findFirst().orElseThrow();
        assertTrue(salary.getNarration().contains("TATA CONSULTANCY SERVICES LTD"));
        assertEquals(new BigDecimal("45000.00"), salary.getCredit());
        assertEquals(5, salary.getDate().getDayOfMonth());
        assertRunningBalance(statement.getOpeningBalance(), txns);
    }

    @Test
    void rejectsWrongSalaryCompany() {
        Statement statement = sampleStatement("SBI");
        List<LlmTransactionItem> items = List.of(
                salaryItem("2026-04-05", "NEFT CR INFOSYS LIMITED"),
                upiDebit("2026-04-10", 1800)
        );
        statement.getTransactionSettings().setMinTransactions(1);
        statement.getTransactionSettings().setMaxTransactions(1);

        assertThrows(IllegalArgumentException.class,
                () -> validationService.validateAndConvert(statement, items));
    }

    @Test
    void removesDuplicateRows() {
        Statement statement = sampleStatement("KOTAK");
        statement.getTransactionSettings().setMinTransactions(2);
        statement.getTransactionSettings().setMaxTransactions(2);
        LlmTransactionItem dup = upiDebit("2026-04-10", 500);
        List<LlmTransactionItem> items = List.of(
                salaryItem("2026-04-05", "NEFT CR TATA CONSULTANCY SERVICES LTD"),
                salaryItem("2026-05-05", "NEFT CR TATA CONSULTANCY SERVICES LTD"),
                dup, dup, upiDebit("2026-04-12", 600)
        );

        List<Transaction> txns = validationService.validateAndConvert(statement, items);
        long activity = txns.stream().filter(t -> !"SAL".equals(t.getType())).count();
        assertEquals(2, activity);
    }

    private static LlmTransactionItem salaryItem(String date, String description) {
        LlmTransactionItem item = new LlmTransactionItem();
        item.setDate(date);
        item.setType("credit");
        item.setAmount(new BigDecimal("45000"));
        item.setDescription(description);
        item.setChannel("salary");
        return item;
    }

    private static LlmTransactionItem upiDebit(String date, int amount) {
        LlmTransactionItem item = new LlmTransactionItem();
        item.setDate(date);
        item.setType("debit");
        item.setAmount(new BigDecimal(amount));
        item.setDescription("UPI/998276542190/MERCHANT/SBIN");
        item.setChannel("upi");
        return item;
    }

    private static Statement sampleStatement(String bankCode) {
        TransactionSettings settings = TransactionSettings.builder()
                .salary(true)
                .salaryCompanyName("TATA CONSULTANCY SERVICES LTD")
                .salaryAmount(new BigDecimal("45000"))
                .salaryDayOfMonth(5)
                .upi(true)
                .atm(false)
                .emi(false)
                .interest(false)
                .minTransactions(8)
                .maxTransactions(8)
                .build();

        return Statement.builder()
                .bankCode(bankCode)
                .openingBalance(new BigDecimal("120000.00"))
                .period(StatementPeriod.builder()
                        .fromDate(LocalDate.of(2026, 4, 1))
                        .toDate(LocalDate.of(2026, 5, 31))
                        .build())
                .transactionSettings(settings)
                .accountDetails(AccountDetails.builder().accountNumber("1234567890").build())
                .build();
    }

    private static void assertRunningBalance(BigDecimal opening, List<Transaction> txns) {
        BigDecimal running = opening.setScale(2);
        for (Transaction txn : txns) {
            BigDecimal credit = txn.getCredit() != null ? txn.getCredit() : BigDecimal.ZERO;
            BigDecimal debit = txn.getDebit() != null ? txn.getDebit() : BigDecimal.ZERO;
            running = running.add(credit).subtract(debit);
            assertEquals(0, running.compareTo(txn.getBalance()),
                    "Balance mismatch on " + txn.getDate() + ": expected " + running + " got " + txn.getBalance());
        }
    }
}
