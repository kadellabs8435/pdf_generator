package com.bankstatement.service.transaction;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Random;

/**
 * FinBox-accepted Kotak narration patterns (UPI-dominant, limited IMPS/NEFT, monthly interest).
 */
final class KotakNarrationGenerator {

    private final Random random;
    private final KotakEcosystemContext ecosystem;

    KotakNarrationGenerator(Random random, KotakEcosystemContext ecosystem) {
        this.random = random;
        this.ecosystem = ecosystem;
    }

    KotakTxnEntry forChannel(KotakChannel channel) {
        return switch (channel) {
            case UPI_DEBIT -> upiDebit(ecosystem.nextName());
            case UPI_CREDIT -> upiCredit(ecosystem.nextName());
            case UPI_CR -> upiCr(ecosystem.nextName());
            case UPI_REV -> upiRev(ecosystem.nextName());
            case IMPS -> imps(ecosystem.nextName());
            case NEFT -> neft(ecosystem.nextName());
            case INT_CREDIT -> interestCredit(KotakAcceptedAmountRules.interestAmount(random));
        };
    }

    KotakTxnEntry upiDebit(String name) {
        String txnId = randomDigits(12);
        String narration = "UPI/" + name + "/" + txnId + "/UPI";
        return new KotakTxnEntry(narration, "UPI-" + txnId, "UPI", Direction.DEBIT);
    }

    KotakTxnEntry upiCredit(String name) {
        return credit("UPI CREDIT/" + name, null, "UPI");
    }

    KotakTxnEntry upiCr(String name) {
        String txnId = randomDigits(12);
        return credit("UPI-CR/" + name + "/" + txnId, "UPI-" + txnId, "UPI");
    }

    KotakTxnEntry upiRev(String name) {
        String txnId = randomDigits(12);
        return credit("UPI/REV/" + name + "/" + txnId, "UPI-" + txnId, "UPI");
    }

    KotakTxnEntry imps(String name) {
        return credit("IMPS/" + name + "/Transfer", "IMPS-" + randomDigits(12), "IMPS");
    }

    KotakTxnEntry neft(String name) {
        return credit("NEFT/" + name + "/Transfer", "NEFT-" + randomDigits(12), "NEFT");
    }

    KotakTxnEntry interestCredit(BigDecimal amount) {
        String narration = KotakAcceptedAmountRules.interestNarration(amount);
        return new KotakTxnEntry(narration, null, "INT", Direction.CREDIT, amount);
    }

    KotakTxnEntry forChannel(KotakChannel channel, ActivityFlowPlanner.Direction direction) {
        if (direction == ActivityFlowPlanner.Direction.DEBIT || channel == KotakChannel.UPI_DEBIT) {
            return upiDebit(ecosystem.nextName());
        }
        return forChannel(channel);
    }

    static boolean isCreditNarration(String narration) {
        return directionOf(narration) == Direction.CREDIT;
    }

    static boolean isDebitNarration(String narration) {
        return directionOf(narration) == Direction.DEBIT;
    }

    static Direction directionOf(String narration) {
        if (narration == null || narration.isBlank()) {
            return Direction.UNKNOWN;
        }
        String upper = narration.toUpperCase(Locale.ROOT);
        if (upper.startsWith("INT. CREDIT") || upper.startsWith("UPI CREDIT/")
                || upper.startsWith("UPI-CR/") || upper.startsWith("UPI/REV/")) {
            return Direction.CREDIT;
        }
        if (upper.contains("SALARY CREDIT /") || upper.startsWith("NEFT SALARY")
                || upper.startsWith("PAYROLL /")) {
            return Direction.CREDIT;
        }
        if (upper.startsWith("IMPS/") || upper.startsWith("NEFT/")) {
            return Direction.CREDIT;
        }
        if (upper.startsWith("UPI/")) {
            return Direction.DEBIT;
        }
        return Direction.UNKNOWN;
    }

    static FinBoxAmountRules.Merchant merchantFromNarration(String narration) {
        return FinBoxAmountRules.Merchant.FRIEND;
    }

    private KotakTxnEntry credit(String narration, String reference, String type) {
        return new KotakTxnEntry(narration, reference, type, Direction.CREDIT, null);
    }

    private String randomDigits(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    enum Direction {
        CREDIT, DEBIT, UNKNOWN
    }

    record KotakTxnEntry(String narration, String reference, String type, Direction direction,
                         BigDecimal fixedAmount) {
        KotakTxnEntry(String narration, String reference, String type, Direction direction) {
            this(narration, reference, type, direction, null);
        }
    }
}
