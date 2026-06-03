package com.bankstatement.service.transaction;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

/** Realistic Bank of India statement remark patterns. */
final class BoiNarrationGenerator {

    private static final String[] ATM_LOCATIONS = {
            "BIAORA", "RAJGARH", "BHOPAL", "INDORE", "UJJAIN", "GUNA", "DEWAS", "SHIVPURI"
    };

    private static final String[] NEFT_COMPANIES = {
            "TATA STEEL LIMITED", "INFOSYS LIMITED", "TCS LIMITED", "WIPRO LIMITED", "HDFC BANK LIMITED"
    };

    private static final String[] CHARGE_TYPES = {
            "SMS CHARGES", "ATM CHARGES"
    };

    /** Merchant / POS debit labels — only used when UPI is enabled on the generator form. */
    private static final String[] MERCHANT_DEBIT_LABELS = {
            "POS PURCHASE", "SWIGGY", "ZOMATO", "AMAZON", "FLIPKART", "FASTAG", "JIO", "AIRTEL"
    };

    private static final String BOI_CODE = "BKID";

    private BoiNarrationGenerator() {}

    record BoiTxnEntry(String narration, String reference, String type) {}

    static boolean isMerchantDebitLabel(String narration) {
        if (narration == null || narration.isBlank()) {
            return false;
        }
        String upper = narration.toUpperCase(Locale.ROOT);
        for (String label : MERCHANT_DEBIT_LABELS) {
            if (label.equals(upper)) {
                return true;
            }
        }
        return false;
    }

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
        return new BoiTxnEntry("UPI/" + party + "/" + mobile(random), upiReference(random), "UPI");
    }

    static BoiTxnEntry upiCredit(Random random) {
        return incomingCredit(random);
    }

    static BoiTxnEntry merchantQrDebit(Random random) {
        if (random.nextInt(100) < 45) {
            return new BoiTxnEntry(pick(MERCHANT_DEBIT_LABELS, random), upiReference(random), "UPI");
        }
        String merchant = pick(MERCHANT_DEBIT_LABELS, random);
        return new BoiTxnEntry(String.format("UPI/%s/DR/%s/%s/%s/Sent u",
                upiReference(random), merchant, BOI_CODE, mobile(random)), upiReference(random), "UPI");
    }

    static BoiTxnEntry neftCredit(Random random) {
        String company = pick(NEFT_COMPANIES, random);
        return new BoiTxnEntry("BY TRANSFER-NEFT/" + company, neftReference(random), "NEFT");
    }

    static BoiTxnEntry impsCredit(Random random) {
        return new BoiTxnEntry("BY TRANSFER-IMPS/REF" + digits(random, 6), impsReference(random), "IMPS");
    }

    static BoiTxnEntry impsDebit(Random random) {
        return new BoiTxnEntry("TO TRANSFER-IMPS/REF" + digits(random, 6), impsReference(random), "IMPS");
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

    static BankRemarkGenerator.SalaryRemark configuredSalaryUnique(String companyName, Random random,
                                                                     Set<String> used) {
        for (int attempt = 0; attempt < 30; attempt++) {
            BankRemarkGenerator.SalaryRemark remark = configuredSalary(companyName, random);
            if (used.add(remark.narration())) {
                return remark;
            }
        }
        BankRemarkGenerator.SalaryRemark remark = configuredSalary(companyName, random);
        return new BankRemarkGenerator.SalaryRemark(
                remark.narration() + random.nextInt(1000), remark.reference(), remark.type());
    }

    static String neftCreditWithCompany(String companyName, Random random) {
        return configuredSalary(companyName, random).narration();
    }

    static BoiTxnEntry neftDebit(Random random) {
        String party = IndianNamesPool.pickName(random);
        return new BoiTxnEntry("TO TRANSFER-NEFT/" + party, neftReference(random), "NEFT");
    }

    static BoiTxnEntry atmWithdrawal(Random random) {
        return new BoiTxnEntry("ATM CASH WDL/" + pick(ATM_LOCATIONS, random), atmReference(random), "ATM");
    }

    static String salaryCreditFixed(Random random) {
        return configuredSalary("EMPLOYER", random).narration();
    }

    static String upiSalaryCredit(Random random) {
        return configuredSalary("EMPLOYER", random).narration();
    }

    static String neftSalaryCredit(Random random) {
        return configuredSalary("EMPLOYER", random).narration();
    }

    static String emiDebit(Random random) {
        return SbiNarrationGenerator.emiDebit(random).narration();
    }

    static String interestCredit(String accountNumber, LocalDate from, LocalDate to, Random random) {
        return "INT.PD";
    }

    static String bankCharge(Random random) {
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
        if (narration == null || narration.isBlank()) {
            return false;
        }
        String upper = narration.toUpperCase(Locale.ROOT);
        return upper.startsWith("BY TRANSFER-") || upper.equals("INT.PD");
    }

    static boolean isDebitNarration(String narration) {
        if (narration == null || narration.isBlank()) {
            return false;
        }
        String upper = narration.toUpperCase(Locale.ROOT);
        return upper.startsWith("TO TRANSFER-")
                || upper.startsWith("UPI/")
                || upper.startsWith("ATM CASH WDL")
                || upper.equals("SMS CHARGES")
                || upper.equals("ATM CHARGES")
                || isMerchantDebitLabel(narration)
                || upper.startsWith("ECS/")
                || upper.startsWith("ACH DEBIT/")
                || upper.startsWith("EMI PAYMENT/");
    }

    private static String atmReference(Random random) {
        return "ATM" + digits(random, 8);
    }

    private static String mobile(Random random) {
        return digits(random, 10);
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
