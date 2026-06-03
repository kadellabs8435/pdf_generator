package com.bankstatement.service.transaction;

import com.bankstatement.entity.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

/**
 * Ensures every transaction row has exactly one populated amount column for rendering.
 * Uses {@link Transaction#getRenderCredit()}/{@link Transaction#getRenderDebit()} when balance
 * processing cleared both columns without changing running balances.
 */
public final class TransactionAmountGuard {

    private static final Logger log = LoggerFactory.getLogger(TransactionAmountGuard.class);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private TransactionAmountGuard() {}

    public record AmountSnapshot(BigDecimal credit, BigDecimal debit) {
        static AmountSnapshot from(Transaction txn) {
            return new AmountSnapshot(positive(txn.getCredit()), positive(txn.getDebit()));
        }
    }

    public static List<AmountSnapshot> snapshot(List<Transaction> transactions) {
        return transactions.stream().map(AmountSnapshot::from).toList();
    }

    /**
     * Restores display amounts from snapshots where both columns were cleared.
     * Does not modify {@link Transaction#getBalance()} values.
     */
    public static void restoreDisplayAmounts(List<Transaction> transactions, List<AmountSnapshot> snapshots) {
        if (transactions == null || snapshots == null) {
            return;
        }
        int limit = Math.min(transactions.size(), snapshots.size());
        for (int i = 0; i < limit; i++) {
            restoreDisplayAmount(transactions.get(i), snapshots.get(i));
        }
    }

    public static void validateForRender(List<Transaction> transactions) {
        if (transactions == null) {
            return;
        }
        for (int i = 0; i < transactions.size(); i++) {
            Transaction txn = transactions.get(i);
            if (!hasPopulatedDisplayAmount(txn) && hasNarration(txn)) {
                log.warn("Transaction row {} missing debit/credit despite narration: {}",
                        i + 1, txn.getNarration());
            }
        }
    }

    /** Prepares transactions for PDF/HTML export and API responses. */
    public static void prepareForRender(List<Transaction> transactions, BigDecimal openingBalance) {
        restoreFromBalanceDelta(transactions, openingBalance);
        enrichForRender(transactions);
        validateForRender(transactions);
    }

    /** Copies render-only amounts into credit/debit for consumers that read those fields directly. */
    public static void enrichForRender(List<Transaction> transactions) {
        if (transactions == null) {
            return;
        }
        transactions.forEach(TransactionAmountGuard::enrichForRender);
    }

    public static void enrichForRender(Transaction txn) {
        if (txn == null || hasPopulatedAmount(txn) || !hasNarration(txn)) {
            return;
        }
        BigDecimal credit = displayCredit(txn);
        BigDecimal debit = displayDebit(txn);
        if (credit.compareTo(ZERO) > 0) {
            txn.setCredit(credit);
            txn.setDebit(ZERO);
        } else if (debit.compareTo(ZERO) > 0) {
            txn.setDebit(debit);
            txn.setCredit(ZERO);
        }
    }

    public static BigDecimal displayCredit(Transaction txn) {
        if (txn == null) {
            return ZERO;
        }
        BigDecimal render = positive(txn.getRenderCredit());
        if (render.compareTo(ZERO) > 0) {
            return render;
        }
        return positive(txn.getCredit());
    }

    public static BigDecimal displayDebit(Transaction txn) {
        if (txn == null) {
            return ZERO;
        }
        BigDecimal render = positive(txn.getRenderDebit());
        if (render.compareTo(ZERO) > 0) {
            return render;
        }
        return positive(txn.getDebit());
    }

    public static void restoreFromBalanceDelta(List<Transaction> transactions, BigDecimal openingBalance) {
        if (transactions == null || openingBalance == null) {
            return;
        }
        BigDecimal previousBalance = scale(openingBalance);
        for (Transaction txn : transactions) {
            if (txn.getBalance() == null) {
                continue;
            }
            BigDecimal balance = scale(txn.getBalance());
            if (!hasPopulatedDisplayAmount(txn) && hasNarration(txn)) {
                BigDecimal delta = scale(balance.subtract(previousBalance));
                if (delta.compareTo(ZERO) > 0) {
                    txn.setRenderCredit(delta);
                    txn.setRenderDebit(ZERO);
                } else if (delta.compareTo(ZERO) < 0) {
                    txn.setRenderDebit(delta.abs());
                    txn.setRenderCredit(ZERO);
                }
            }
            previousBalance = balance;
        }
    }

    static void restoreDisplayAmount(Transaction txn, AmountSnapshot snapshot) {
        if (txn == null || snapshot == null || hasPopulatedDisplayAmount(txn) || !hasNarration(txn)) {
            return;
        }
        BigDecimal amount = resolveAmount(txn, snapshot);
        if (amount.compareTo(ZERO) <= 0) {
            log.warn("Unable to restore amount for narration: {}", txn.getNarration());
            return;
        }
        if (isCreditTransaction(txn)) {
            txn.setRenderCredit(amount);
            txn.setRenderDebit(ZERO);
        } else {
            txn.setRenderDebit(amount);
            txn.setRenderCredit(ZERO);
        }
    }

    private static BigDecimal resolveAmount(Transaction txn, AmountSnapshot snapshot) {
        if (isCreditTransaction(txn)) {
            if (snapshot.credit().compareTo(ZERO) > 0) {
                return snapshot.credit();
            }
            if (snapshot.debit().compareTo(ZERO) > 0) {
                return snapshot.debit();
            }
        } else {
            if (snapshot.debit().compareTo(ZERO) > 0) {
                return snapshot.debit();
            }
            if (snapshot.credit().compareTo(ZERO) > 0) {
                return snapshot.credit();
            }
        }
        return ZERO;
    }

    public static boolean isCreditTransaction(Transaction txn) {
        if (txn == null) {
            return false;
        }
        String type = txn.getType() != null ? txn.getType().toUpperCase(Locale.ROOT) : "";
        if ("SAL".equals(type) || "INT".equals(type) || "IMPS".equals(type) || "NEFT".equals(type)) {
            return true;
        }
        String narration = txn.getNarration();
        if (narration == null || narration.isBlank()) {
            return false;
        }
        if (KotakNarrationGenerator.isCreditNarration(narration)) {
            return true;
        }
        if (SbiNarrationGenerator.isCreditNarration(narration)) {
            return true;
        }
        if (BoiNarrationGenerator.isCreditNarration(narration)) {
            return true;
        }
        return BankRemarkGenerator.isCreditNarration(narration);
    }

    private static boolean hasPopulatedAmount(Transaction txn) {
        return positive(txn.getCredit()).compareTo(ZERO) > 0
                || positive(txn.getDebit()).compareTo(ZERO) > 0;
    }

    private static boolean hasPopulatedDisplayAmount(Transaction txn) {
        return displayCredit(txn).compareTo(ZERO) > 0
                || displayDebit(txn).compareTo(ZERO) > 0;
    }

    private static boolean hasNarration(Transaction txn) {
        return txn.getNarration() != null && !txn.getNarration().isBlank();
    }

    private static BigDecimal positive(BigDecimal amount) {
        if (amount == null || amount.compareTo(ZERO) <= 0) {
            return ZERO;
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal scale(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
