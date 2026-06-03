package com.bankstatement.service.transaction;

import com.bankstatement.entity.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransactionAmountGuardTest {

    @Test
    void restoresCreditWhenBothColumnsClearedAfterRecalculate() {
        Transaction txn = Transaction.builder()
                .date(LocalDate.of(2025, 1, 10))
                .narration("UPI-CR/HEMANT KULKARNI/607577155414")
                .reference("UPI-607577155414")
                .type("UPI")
                .credit(BigDecimal.ZERO.setScale(2))
                .debit(BigDecimal.ZERO.setScale(2))
                .renderCredit(new BigDecimal("24500.00"))
                .balance(new BigDecimal("120000.00"))
                .build();

        TransactionAmountGuard.enrichForRender(txn);

        assertEquals(new BigDecimal("24500.00"), txn.getCredit());
        assertEquals(new BigDecimal("120000.00"), txn.getBalance());
        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(txn.getDebit()));
    }

    @Test
    void classifiesKotakNeftAsCredit() {
        assertTrue(TransactionAmountGuard.isCreditTransaction(Transaction.builder()
                .narration("NEFT/POOJA HEGDE/Transfer")
                .type("NEFT")
                .build()));
    }

    @Test
    void classifiesSbiDebitTransfer() {
        assertFalse(TransactionAmountGuard.isCreditTransaction(Transaction.builder()
                .narration("TO TRANSFER-UPI/RAJESH SHARMA")
                .type("UPI")
                .build()));
    }

    @Test
    void restoreFromBalanceDeltaFillsMissingDebit() {
        Transaction txn = Transaction.builder()
                .date(LocalDate.of(2025, 1, 11))
                .narration("TO TRANSFER-UPI/RAJESH SHARMA")
                .type("UPI")
                .credit(BigDecimal.ZERO.setScale(2))
                .debit(BigDecimal.ZERO.setScale(2))
                .balance(new BigDecimal("95000.00"))
                .build();

        TransactionAmountGuard.restoreFromBalanceDelta(
                List.of(txn), new BigDecimal("120000.00"));
        TransactionAmountGuard.enrichForRender(txn);

        assertEquals(new BigDecimal("25000.00"), txn.getDebit());
        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(txn.getCredit()));
    }
}
