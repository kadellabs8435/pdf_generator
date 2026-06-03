package com.bankstatement.service.transaction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * Realistic Indian bank statement remarks shared across banks.
 * Bank-specific narrations: {@link SbiNarrationGenerator}, {@link BoiNarrationGenerator},
 * {@link KotakNarrationGenerator}.
 */
final class BankRemarkGenerator {

    private static final DateTimeFormatter SALARY_MONTH =
            DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    private static final String[] ATM_LOCATIONS = {
            "BIORA SSI", "BHOPAL", "INDORE", "UJJAIN", "RAJGARH", "GUNA", "DEWAS", "SHIVPURI"
    };

    private static final String[] EMI_LENDERS = {
            "BAJAJ FINSERV", "HDFC CAR LOAN", "TATA CAPITAL", "ICICI BANK", "AXIS BANK"
    };

    private static final String[] UTILITIES = {
            "ELECTRICITY BILL MPPKVVCL", "MOBILE RECHARGE JIO", "MOBILE RECHARGE AIRTEL",
            "FASTAG RECHARGE", "INSURANCE PREMIUM", "GAS BILL IGL", "WATER BILL BMC"
    };

    private BankRemarkGenerator() {}

    record SalaryRemark(String narration, String reference, String type) {}

    static String resolveBankCode(String bankCode) {
        if (bankCode == null) {
            return "SBIN";
        }
        return switch (bankCode.toUpperCase(Locale.ROOT)) {
            case "BOI" -> "BKID";
            case "KOTAK" -> "KKBK";
            case "SBI" -> "SBIN";
            default -> "SBIN";
        };
    }

    /** Kotak salary — only stable employer-style narrations. */
    static SalaryRemark kotakConfiguredSalary(String companyName, Random random) {
        String company = normalizeCompany(companyName);
        return switch (random.nextInt(3)) {
            case 0 -> new SalaryRemark("SALARY CREDIT / " + company, null, "SAL");
            case 1 -> new SalaryRemark("PAYROLL / " + company, null, "SAL");
            default -> new SalaryRemark("NEFT SALARY / " + company, "NEFTINW-" + digits(random, 10), "SAL");
        };
    }

    static SalaryRemark configuredSalary(String companyName, LocalDate creditDate, Random random) {
        String company = normalizeCompany(companyName);
        return switch (random.nextInt(4)) {
            case 0 -> new SalaryRemark("NEFT SALARY / " + company, "NEFTINW-" + digits(random, 10), "SAL");
            case 1 -> new SalaryRemark("SALARY CREDIT / " + company, null, "SAL");
            case 2 -> new SalaryRemark("PAYROLL / " + company, null, "SAL");
            default -> new SalaryRemark("SALARY FOR " + creditDate.format(SALARY_MONTH).toUpperCase(Locale.ENGLISH)
                    + " / " + company, null, "SAL");
        };
    }

    static SalaryRemark legacySalary(LocalDate creditDate, Random random) {
        return configuredSalary("EMPLOYER", creditDate, random);
    }

    static SalaryRemark configuredSalaryUnique(String companyName, LocalDate creditDate, Random random,
                                               Set<String> used) {
        for (int attempt = 0; attempt < 30; attempt++) {
            SalaryRemark remark = configuredSalary(companyName, creditDate, random);
            if (used.add(remark.narration())) {
                return remark;
            }
        }
        SalaryRemark remark = configuredSalary(companyName, creditDate, random);
        return new SalaryRemark(remark.narration() + random.nextInt(1000), remark.reference(), remark.type());
    }

    static String upiDebit(Random random, String bankCode, FinBoxAmountRules.Merchant merchant) {
        return upiDebit(random, bankCode, merchant, null);
    }

    static String upiDebit(Random random, String bankCode, FinBoxAmountRules.Merchant merchant, String partyOverride) {
        String code = resolveBankCode(bankCode);
        String merchantName = partyOverride != null && !partyOverride.isBlank()
                ? partyOverride
                : merchantLabel(merchant, random);
        return String.format("UPI/%s/%s/%s", digits(random, 12), merchantName, code);
    }

    static String upiDebit(Random random, String bankCode) {
        return upiDebit(random, bankCode, FinBoxAmountRules.randomDebitMerchant(random));
    }

    static String smallIncomingCredit(Random random, String bankCode) {
        return smallIncomingCredit(random, bankCode, IndianNamesPool.pickName(random));
    }

    static String smallIncomingCredit(Random random, String bankCode, String person) {
        String code = resolveBankCode(bankCode);
        return switch (random.nextInt(4)) {
            case 0 -> String.format("Recd:IMPS/%s/%s/%s/X%s/NA",
                    digits(random, 12), person, code, digits(random, 4));
            case 1 -> "IMPS CR FROM " + person;
            case 2 -> "NEFTINW/TRANSFER FROM " + person;
            default -> "UPI/" + person + "/" + digits(random, 12) + "/Payment received";
        };
    }

    static String upiRefundCredit(Random random) {
        String person = IndianNamesPool.pickName(random);
        return "UPI/" + person + "/" + digits(random, 12) + "/UPI refund";
    }

    static String atmWithdrawal(Random random, String bankCode) {
        String code = resolveBankCode(bankCode);
        return switch (random.nextInt(3)) {
            case 0 -> String.format("ATM WDL/%s/%s", pick(ATM_LOCATIONS, random), digits(random, 4));
            case 1 -> String.format("ATM CASH WDL/%s/%s", pick(ATM_LOCATIONS, random), digits(random, 4));
            default -> String.format("CASH WDL ATM/%s/%s", code, digits(random, 4));
        };
    }

    static String emiDebit(Random random) {
        return switch (random.nextInt(3)) {
            case 0 -> "ECS/" + pick(EMI_LENDERS, random) + " EMI";
            case 1 -> "ACH DEBIT/" + pick(EMI_LENDERS, random);
            default -> "EMI PAYMENT/" + pick(EMI_LENDERS, random);
        };
    }

    static String interestCredit(Random random) {
        return switch (random.nextInt(3)) {
            case 0 -> "SB A/C INTEREST CREDIT";
            case 1 -> "QUARTERLY INTEREST CREDIT";
            default -> "INT CREDIT SAVINGS ACCOUNT";
        };
    }

    static String utilityDebit(Random random) {
        return pick(UTILITIES, random);
    }

    static String retailDebit(Random random, String bankCode) {
        FinBoxAmountRules.Merchant merchant = FinBoxAmountRules.randomDebitMerchant(random);
        if (merchant == FinBoxAmountRules.Merchant.UTILITY) {
            return utilityDebit(random);
        }
        if (merchant == FinBoxAmountRules.Merchant.EMI) {
            return emiDebit(random);
        }
        if (merchant == FinBoxAmountRules.Merchant.ATM) {
            return atmWithdrawal(random, bankCode);
        }
        return upiDebit(random, bankCode, merchant);
    }

    static boolean isCreditNarration(String narration) {
        if (narration == null || narration.isBlank()) {
            return false;
        }
        if (SbiNarrationGenerator.isCreditNarration(narration)
                || BoiNarrationGenerator.isCreditNarration(narration)
                || KotakNarrationGenerator.isCreditNarration(narration)) {
            return true;
        }
        String upper = narration.toUpperCase(Locale.ROOT);
        return upper.contains("PAYMENT RECEIVED") || upper.contains("UPI REFUND");
    }

    static boolean isBannedEmployerCreditText(String narration) {
        if (narration == null) {
            return false;
        }
        String upper = narration.toUpperCase(Locale.ROOT);
        if (upper.contains("INSTALLMENT") || upper.contains("MUTHOOTFINCORPL")) {
            return false;
        }
        if (upper.startsWith("NEFT AXNGG") && upper.contains("GOOGLE INDIA")) {
            return false;
        }
        if (upper.startsWith("UPI/") && (upper.contains("FLIPKART") || upper.contains("MEESHO")
                || upper.contains("GOOGLE INDIA") || upper.contains("PAYU") || upper.contains("JIO RECHARGE"))) {
            return false;
        }
        if (upper.startsWith("SWEEP TRF FROM")) {
            return false;
        }
        return upper.contains("MUTHOOTFIN")
                || upper.contains("INFOSYS")
                || upper.contains("GOOGLEINDI")
                || (upper.contains("GOOGLE INDIA") && !upper.contains("DIGITAL SERVIC"))
                || upper.contains("TCS LIMITED")
                || upper.contains(" TCS")
                || upper.contains("WIPRO")
                || upper.contains("CAPGEMINI");
    }

    private static String merchantLabel(FinBoxAmountRules.Merchant merchant, Random random) {
        return switch (merchant) {
            case AIRTEL -> "AIRTEL";
            case JIO -> "JIO";
            case SWIGGY -> "SWIGGY";
            case ZOMATO -> "ZOMATO";
            case DMART -> "DMART";
            case IRCTC -> "IRCTC";
            case FRIEND -> IndianNamesPool.pickName(random);
            default -> pick(new String[]{"DMART", "SWIGGY", "JIO", "AIRTEL", "IRCTC"}, random);
        };
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
