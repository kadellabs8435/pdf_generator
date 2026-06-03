package com.bankstatement.service.transaction;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

/** Realistic Bank of India statement remark patterns. */
final class BoiNarrationGenerator {

    private static final String[] CHARGE_TYPES = {
            "SMSChrgsJAN-MAR26+GST+101", "SMSChrgsAPR-JUN26+GST+98", "ATMCHG+GST", "AMBCHG+GST",
            "IMPSChrgs+GST", "NEFTChrgs+GST", "AnnualMaint+GST"
    };

    private BoiNarrationGenerator() {}

    record BoiTxnEntry(String narration, String reference, String type) {}

    static String unique(Supplier<String> supplier, Set<String> used, Random random) {
        for (int attempt = 0; attempt < 30; attempt++) {
            String narration = supplier.get();
            if (used.add(narration)) {
                return narration;
            }
        }
        return supplier.get() + random.nextInt(1000);
    }

    static BoiTxnEntry upiDebit(Random random) {
        String party = IndianNamesPool.pickName(random);
        return new BoiTxnEntry("TO TRANSFER-UPI/" + party, upiReference(random), "UPI");
    }

    static BoiTxnEntry upiCredit(Random random) {
        return incomingCredit(random);
    }

    static BoiTxnEntry merchantQrDebit(Random random) {
        return upiDebit(random);
    }

    static BoiTxnEntry neftCredit(Random random) {
        String party = IndianNamesPool.pickName(random);
        return new BoiTxnEntry("BY TRANSFER-NEFT/" + party, neftReference(random), "NEFT");
    }

    static BoiTxnEntry impsCredit(Random random) {
        String party = IndianNamesPool.pickName(random);
        return new BoiTxnEntry("BY TRANSFER-IMPS/" + party, impsReference(random), "IMPS");
    }

    static BoiTxnEntry incomingCredit(Random random) {
        return switch (random.nextInt(3)) {
            case 0 -> {
                String party = IndianNamesPool.pickName(random);
                yield new BoiTxnEntry("BY TRANSFER-UPI/" + party, upiReference(random), "UPI");
            }
            case 1 -> impsCredit(random);
            default -> neftCredit(random);
        };
    }

    static BankRemarkGenerator.SalaryRemark configuredSalary(String companyName, Random random) {
        String company = normalizeCompany(companyName);
        return new BankRemarkGenerator.SalaryRemark(
                "BY TRANSFER-SALARY/" + company,
                neftReference(random),
                "SAL");
    }

    static String neftCreditWithCompany(String companyName, Random random) {
        return configuredSalary(companyName, random).narration();
    }

    static BoiTxnEntry neftDebit(Random random) {
        String party = IndianNamesPool.pickName(random);
        return new BoiTxnEntry("TO TRANSFER-NEFT/" + party, neftReference(random), "NEFT");
    }

    static BoiTxnEntry atmWithdrawal(Random random) {
        return new BoiTxnEntry("ATM CASH WDL", atmReference(random), "ATM");
    }

    static String salaryCreditFixed(Random random) {
        return BankRemarkGenerator.legacySalary(LocalDate.now(), random).narration();
    }

    static String upiSalaryCredit(Random random) {
        return BankRemarkGenerator.legacySalary(LocalDate.now(), random).narration();
    }

    static String neftSalaryCredit(Random random) {
        return BankRemarkGenerator.legacySalary(LocalDate.now(), random).narration();
    }

    static String emiDebit(Random random) {
        return SbiNarrationGenerator.emiDebit(random).narration();
    }

    static String interestCredit(String accountNumber, LocalDate from, LocalDate to, Random random) {
        return "INT. CREDIT";
    }

    static String bankCharge(Random random) {
        if (random.nextInt(100) < 35) {
            return BankRemarkGenerator.utilityDebit(random);
        }
        return pick(CHARGE_TYPES, random);
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
        return SbiNarrationGenerator.isCreditNarration(narration);
    }

    static boolean isDebitNarration(String narration) {
        return SbiNarrationGenerator.isDebitNarration(narration);
    }

    private static String atmReference(Random random) {
        return "ATM" + digits(random, 8);
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

    private static String pick(String[] arr, Random random) {
        return arr[random.nextInt(arr.length)];
    }
}
