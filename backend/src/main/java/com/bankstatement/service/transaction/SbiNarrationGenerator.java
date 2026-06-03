package com.bankstatement.service.transaction;

import java.util.Locale;
import java.util.Random;

/** Realistic State Bank of India statement narration patterns. */
final class SbiNarrationGenerator {

    private SbiNarrationGenerator() {}

    record SbiTxnEntry(String narration, String reference, String type) {}

    static SbiTxnEntry upiDebit(Random random, String party) {
        return new SbiTxnEntry("TO TRANSFER-UPI/" + party, upiReference(random), "UPI");
    }

    static SbiTxnEntry upiCredit(Random random, String party) {
        return new SbiTxnEntry("BY TRANSFER-UPI/" + party, upiReference(random), "UPI");
    }

    static SbiTxnEntry impsCredit(Random random, String party) {
        return new SbiTxnEntry("BY TRANSFER-IMPS/" + party, impsReference(random), "IMPS");
    }

    static SbiTxnEntry neftCredit(Random random, String party) {
        return new SbiTxnEntry("BY TRANSFER-NEFT/" + party, neftReference(random), "NEFT");
    }

    static SbiTxnEntry incomingCredit(Random random, String party) {
        return switch (random.nextInt(3)) {
            case 0 -> upiCredit(random, party);
            case 1 -> impsCredit(random, party);
            default -> neftCredit(random, party);
        };
    }

    static BankRemarkGenerator.SalaryRemark configuredSalary(String companyName, Random random) {
        String company = normalizeCompany(companyName);
        return new BankRemarkGenerator.SalaryRemark(
                "BY TRANSFER-SALARY/" + company,
                neftReference(random),
                "SAL");
    }

    static SbiTxnEntry atmWithdrawal(Random random) {
        return new SbiTxnEntry("ATM CASH WDL", atmReference(random), "ATM");
    }

    static SbiTxnEntry interestCredit(Random random) {
        return new SbiTxnEntry("INTEREST CREDIT", null, "INT");
    }

    static SbiTxnEntry emiDebit(Random random) {
        String lender = pickEmiLender(random);
        return switch (random.nextInt(3)) {
            case 0 -> new SbiTxnEntry("ECS/" + lender + " EMI", ecsReference(random), "EMI");
            case 1 -> new SbiTxnEntry("ACH DEBIT/" + lender, achReference(random), "EMI");
            default -> new SbiTxnEntry("EMI PAYMENT/" + lender, ecsReference(random), "EMI");
        };
    }

    static String upiReference(Random random) {
        return digits(random, 12);
    }

    static String impsReference(Random random) {
        return "IMPS-" + digits(random, 12);
    }

    static String neftReference(Random random) {
        return "NEFTINW-" + digits(random, 10);
    }

    static boolean isCreditNarration(String narration) {
        if (narration == null || narration.isBlank()) {
            return false;
        }
        String upper = narration.toUpperCase(Locale.ROOT);
        return upper.startsWith("BY TRANSFER-")
                || upper.equals("INTEREST CREDIT")
                || upper.contains("SALARY CREDIT")
                || upper.startsWith("NEFT SALARY")
                || upper.startsWith("PAYROLL")
                || upper.startsWith("IMPS CR FROM")
                || upper.startsWith("NEFTINW/TRANSFER")
                || upper.startsWith("RECD:IMPS/");
    }

    static boolean isDebitNarration(String narration) {
        if (narration == null || narration.isBlank()) {
            return false;
        }
        String upper = narration.toUpperCase(Locale.ROOT);
        return upper.startsWith("TO TRANSFER-")
                || upper.equals("ATM CASH WDL")
                || upper.startsWith("ATM WDL/")
                || upper.startsWith("ATM CASH WDL/")
                || upper.startsWith("CASH WDL ATM/")
                || upper.startsWith("ECS/")
                || upper.startsWith("ACH DEBIT/")
                || upper.startsWith("EMI PAYMENT/");
    }

    private static String atmReference(Random random) {
        return "ATM" + digits(random, 8);
    }

    private static String ecsReference(Random random) {
        return "ECS" + digits(random, 10);
    }

    private static String achReference(Random random) {
        return "ACH" + digits(random, 10);
    }

    private static String pickEmiLender(Random random) {
        String[] lenders = {
                "BAJAJ FINSERV", "HDFC CAR LOAN", "TATA CAPITAL", "ICICI BANK", "AXIS BANK"
        };
        return lenders[random.nextInt(lenders.length)];
    }

    private static String normalizeCompany(String companyName) {
        if (companyName == null || companyName.isBlank()) {
            return "EMPLOYER";
        }
        return companyName.trim().toUpperCase(Locale.ROOT);
    }

    private static String digits(Random random, int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
