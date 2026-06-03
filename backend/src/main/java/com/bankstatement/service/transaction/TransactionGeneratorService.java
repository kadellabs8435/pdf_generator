package com.bankstatement.service.transaction;

import com.bankstatement.entity.Statement;
import com.bankstatement.entity.Transaction;
import com.bankstatement.entity.TransactionSettings;
import com.bankstatement.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

@Service
public class TransactionGeneratorService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final int MAX_DEBIT_REGEN_ATTEMPTS = 25;
    private static final int DEBIT_MIN_RUPEES = GeneratedStatementRules.ACTIVITY_DEBIT_MIN;
    private static final int DEBIT_MAX_RUPEES = GeneratedStatementRules.ACTIVITY_DEBIT_MAX;
    private final Random random = new Random();

    public List<Transaction> generate(Statement statement) {
        if (statement.getPeriod() == null || statement.getOpeningBalance() == null) {
            throw new ApiException("Statement period and opening balance are required", HttpStatus.BAD_REQUEST.value());
        }

        TransactionSettings settings = statement.getTransactionSettings();
        if (settings == null) {
            settings = TransactionSettings.builder()
                    .salary(true).upi(true).atm(true).emi(true).interest(true)
                    .minTransactions(8).maxTransactions(20)
                    .build();
        }
        final TransactionSettings txnSettings = settings;

        if (!hasEnabledTransactionType(txnSettings)) {
            throw new ApiException("Select at least one transaction type (salary, UPI, ATM, EMI, or interest)",
                    HttpStatus.BAD_REQUEST.value());
        }

        boolean activityTypes = hasActivityTypesBesidesSalary(txnSettings);

        int min = Math.max(0, txnSettings.getMinTransactions());
        int max = Math.max(min, txnSettings.getMaxTransactions());
        int count = min == max ? min : min + random.nextInt(max - min + 1);
        if (!activityTypes && !hasConfiguredSalary(txnSettings)) {
            count = 1;
        } else if (!activityTypes) {
            count = 0;
        }

        LocalDate from = statement.getPeriod().getFromDate();
        LocalDate to = statement.getPeriod().getToDate();
        long days = ChronoUnit.DAYS.between(from, to);
        if (days < 0) {
            throw new ApiException("Invalid statement period", HttpStatus.BAD_REQUEST.value());
        }

        boolean boi = isBoi(statement.getBankCode());
        boolean kotak = isKotak(statement.getBankCode());
        boolean sbi = isSbi(statement.getBankCode());
        boolean highValue = kotak || sbi;
        validateOpeningBalanceForFloor(scale(statement.getOpeningBalance()), txnSettings,
                statement.getBankCode(), from, to, kotak);
        Set<String> usedNarrations = new HashSet<>();
        List<Transaction> transactions = new ArrayList<>();
        BigDecimal balance = scale(statement.getOpeningBalance());
        String accountNumber = statement.getAccountDetails() != null
                ? statement.getAccountDetails().getAccountNumber() : null;

        if (txnSettings.isSalary()) {
            if (hasConfiguredSalary(txnSettings)) {
                String company = normalizeCompanyName(txnSettings.getSalaryCompanyName());
                BigDecimal salaryAmount = scale(txnSettings.getSalaryAmount());
                int salaryDay = clampSalaryDay(txnSettings.getSalaryDayOfMonth());
                for (LocalDate salaryDate : salaryDatesInPeriod(from, to, salaryDay)) {
                    Transaction salary = buildConfiguredSalaryCredit(
                            salaryDate, company, salaryAmount, balance, boi, kotak, sbi, txnSettings, usedNarrations);
                    transactions.add(salary);
                    balance = applyToBalance(balance, salary);
                }
            } else {
                LocalDate salaryDate = from.plusDays(Math.min(5, (int) days));
                Transaction salary = buildLegacySalaryCredit(
                        salaryDate, balance, boi, kotak, txnSettings, usedNarrations);
                transactions.add(salary);
                balance = applyToBalance(balance, salary);
            }
        }

        KotakEcosystemContext ecosystem = highValue
                ? new KotakEcosystemContext(random, accountNumber) : null;
        KotakNarrationGenerator kotakNarrations = kotak
                ? new KotakNarrationGenerator(random, ecosystem) : null;
        List<ActivityFlowPlanner.Direction> flow = kotak
                ? ActivityFlowPlanner.planDirectionsKotak(count, random)
                : highValue ? ActivityFlowPlanner.planDirections(count, random) : null;
        List<LocalDate> activityDates = highValue
                ? ActivityFlowPlanner.planDates(from, to, count, random) : null;
        Map<YearMonth, Integer> kotakMonthlyCredits = kotak ? new HashMap<>() : null;
        Set<YearMonth> kotakInterestMonths = kotak ? new HashSet<>() : null;
        Set<YearMonth> sbiEmiMonths = sbi ? new HashSet<>() : null;

        for (int i = 0; i < count; i++) {
            LocalDate date = highValue
                    ? activityDates.get(i)
                    : from.plusDays(days == 0 ? 0 : random.nextInt((int) days + 1));
            Transaction txn;
            if (boi) {
                txn = buildBoiTransaction(date, txnSettings, balance, accountNumber, from, to, usedNarrations);
            } else if (kotak) {
                txn = buildKotakDirected(date, flow.get(i), txnSettings, balance,
                        kotakMonthlyCredits, kotakNarrations, ecosystem, kotakInterestMonths);
            } else if (sbi) {
                txn = buildSbiDirected(date, flow.get(i), txnSettings, balance, statement.getBankCode(),
                        ecosystem, sbiEmiMonths);
            } else {
                txn = buildGenericTransaction(date, txnSettings, balance, statement.getBankCode());
            }
            transactions.add(txn);
            balance = applyToBalance(balance, txn);
        }

        Collections.sort(transactions, this::compareTransactionsChronologically);

        recalculateRunningBalances(transactions, scale(statement.getOpeningBalance()), kotak, kotakNarrations);
        TransactionAmountGuard.validateForRender(transactions);

        return transactions;
    }

    /** Date first; salary credits before other same-day rows so the balance floor is reached early. */
    private int compareTransactionsChronologically(Transaction a, Transaction b) {
        int cmp = a.getDate().compareTo(b.getDate());
        if (cmp != 0) {
            return cmp;
        }
        int prioA = chronologicalPriority(a);
        int prioB = chronologicalPriority(b);
        if (prioA != prioB) {
            return Integer.compare(prioA, prioB);
        }
        return a.getReference().compareTo(b.getReference());
    }

    private int chronologicalPriority(Transaction txn) {
        if ("SAL".equals(txn.getType())) {
            return 0;
        }
        if (positiveAmount(txn.getCredit()).compareTo(ZERO) > 0) {
            return 1;
        }
        return 2;
    }

    private boolean isBoi(String bankCode) {
        return bankCode != null && bankCode.equalsIgnoreCase("BOI");
    }

    private boolean isKotak(String bankCode) {
        return bankCode != null && bankCode.equalsIgnoreCase("KOTAK");
    }

    private boolean isSbi(String bankCode) {
        return bankCode != null && bankCode.equalsIgnoreCase("SBI");
    }

    private boolean hasConfiguredSalary(TransactionSettings settings) {
        return settings.isSalary()
                && settings.getSalaryCompanyName() != null
                && !settings.getSalaryCompanyName().isBlank()
                && settings.getSalaryAmount() != null
                && settings.getSalaryAmount().compareTo(ZERO) > 0
                && settings.getSalaryDayOfMonth() != null
                && settings.getSalaryDayOfMonth() >= 1
                && settings.getSalaryDayOfMonth() <= 28;
    }

    /**
     * Opening balance below the floor must be recoverable via at least one minimum credit
     * (salary or activity credit) before debits are allowed.
     */
    private void validateOpeningBalanceForFloor(BigDecimal opening, TransactionSettings settings,
                                                String bankCode, LocalDate from, LocalDate to,
                                                boolean kotak) {
        BigDecimal floor = minRunningBalance(kotak);
        if (opening.compareTo(floor) >= 0) {
            return;
        }
        BigDecimal minCredit = GeneratedStatementRules.ACTIVITY_CREDIT_MIN_BD;
        if (settings.isSalary() && hasConfiguredSalary(settings)) {
            BigDecimal salary = scale(settings.getSalaryAmount());
            if (!salaryDatesInPeriod(from, to, clampSalaryDay(settings.getSalaryDayOfMonth())).isEmpty()
                    && opening.add(salary).compareTo(floor) >= 0) {
                if (kotak && opening.add(salary).compareTo(KotakBalanceRules.MAX) > 0) {
                    throw new ApiException(
                            "Opening balance plus salary must not exceed " + KotakBalanceRules.MAX,
                            HttpStatus.BAD_REQUEST.value());
                }
                return;
            }
        }
        if (opening.add(minCredit).compareTo(floor) >= 0) {
            return;
        }
        throw new ApiException(
                "Opening balance must be at least " + floor
                        + " or high enough that a salary/minimum credit reaches that level",
                HttpStatus.BAD_REQUEST.value());
    }

    private int clampSalaryDay(Integer day) {
        if (day == null) {
            return 1;
        }
        return Math.min(28, Math.max(1, day));
    }

    private List<LocalDate> salaryDatesInPeriod(LocalDate from, LocalDate to, int dayOfMonth) {
        List<LocalDate> dates = new ArrayList<>();
        YearMonth month = YearMonth.from(from);
        YearMonth last = YearMonth.from(to);
        while (!month.isAfter(last)) {
            int day = Math.min(dayOfMonth, month.lengthOfMonth());
            LocalDate creditDate = month.atDay(day);
            if (!creditDate.isBefore(from) && !creditDate.isAfter(to)) {
                dates.add(creditDate);
            }
            month = month.plusMonths(1);
        }
        return dates;
    }

    private Transaction buildConfiguredSalaryCredit(LocalDate date, String company, BigDecimal amount,
                                                    BigDecimal balance, boolean boi, boolean kotak, boolean sbi,
                                                    TransactionSettings settings, Set<String> usedNarrations) {
        BankRemarkGenerator.SalaryRemark salary;
        if (kotak) {
            salary = BankRemarkGenerator.kotakConfiguredSalary(company, random);
        } else if (boi) {
            salary = BoiNarrationGenerator.configuredSalary(company, random);
        } else if (sbi) {
            salary = SbiNarrationGenerator.configuredSalary(company, random);
        } else {
            salary = BankRemarkGenerator.configuredSalary(company, date, random);
        }
        return buildCredit(date, salary.narration(), salary.type(), balance, amount, salary.reference());
    }

    private Transaction buildLegacySalaryCredit(LocalDate date, BigDecimal balance, boolean boi, boolean kotak,
                                                TransactionSettings settings, Set<String> usedNarrations) {
        BankRemarkGenerator.SalaryRemark salary = boi
                ? BankRemarkGenerator.configuredSalaryUnique("EMPLOYER", date, random, usedNarrations)
                : BankRemarkGenerator.legacySalary(date, random);
        BigDecimal amount = kotak
                ? HighValueAmountRules.clusteredCredit(random)
                : randomActivityCreditAmount();
        return buildCredit(date, salary.narration(), salary.type(), balance, amount, salary.reference());
    }

    private String normalizeCompanyName(String companyName) {
        if (companyName == null || companyName.isBlank()) {
            throw new ApiException("Company name is required for salary credits",
                    HttpStatus.BAD_REQUEST.value());
        }
        return companyName.trim();
    }

    private boolean hasEnabledTransactionType(TransactionSettings settings) {
        return settings.isSalary() || settings.isUpi() || settings.isAtm()
                || settings.isEmi() || settings.isInterest();
    }

    private boolean hasActivityTypesBesidesSalary(TransactionSettings settings) {
        return settings.isUpi() || settings.isAtm() || settings.isEmi() || settings.isInterest();
    }

    /** BOI credit kinds allowed by settings (UPI in / SB interest). */
    private List<String> boiCreditKinds(TransactionSettings settings) {
        List<String> kinds = new ArrayList<>();
        if (settings.isUpi()) {
            kinds.add("UPI_CR");
        }
        if (settings.isInterest()) {
            kinds.add("INTEREST");
        }
        return kinds;
    }

    /** BOI debit kinds allowed by settings — each maps to realistic remark patterns only. */
    private List<String> boiDebitKinds(TransactionSettings settings) {
        List<String> kinds = new ArrayList<>();
        if (settings.isUpi()) {
            kinds.add("UPI_DR");
            kinds.add("UPI_DR");
            kinds.add("MERCHANT");
        }
        if (settings.isAtm()) {
            kinds.add("ATM");
        }
        if (settings.isEmi()) {
            kinds.add("EMI");
        }
        if (settings.isInterest()) {
            kinds.add("CHARGE");
        }
        return kinds;
    }

    private List<String> boiKindsForBalance(TransactionSettings settings, BigDecimal balance) {
        if (!canWithdrawStandardDebit(balance)) {
            return boiCreditKinds(settings);
        }
        List<String> kinds = new ArrayList<>(boiDebitKinds(settings));
        if (settings.isUpi()) {
            kinds.add("UPI_CR");
        }
        if (settings.isInterest()) {
            kinds.add("INTEREST");
        }
        return kinds;
    }

    private Transaction buildBoiTransaction(LocalDate date, TransactionSettings settings, BigDecimal balance,
                                            String accountNumber, LocalDate periodFrom, LocalDate periodTo,
                                            Set<String> usedNarrations) {
        List<String> kinds = boiKindsForBalance(settings, balance);
        if (kinds.isEmpty()) {
            return buildBoiCreditFromSettings(date, settings, balance, accountNumber, periodFrom, periodTo,
                    usedNarrations);
        }
        String kind = kinds.get(random.nextInt(kinds.size()));
        java.util.function.Supplier<Transaction> creditFallback = () -> buildBoiCreditFromSettings(
                date, settings, balance, accountNumber, periodFrom, periodTo, usedNarrations);
        return buildBoiKindTransaction(date, settings, balance, kind, accountNumber, periodFrom, periodTo,
                usedNarrations, creditFallback);
    }

    private Transaction buildBoiCreditFromSettings(LocalDate date, TransactionSettings settings, BigDecimal balance,
                                                   String accountNumber, LocalDate periodFrom, LocalDate periodTo,
                                                   Set<String> usedNarrations) {
        List<String> credits = boiCreditKinds(settings);
        if (credits.isEmpty()) {
            throw new ApiException(
                    "Cannot generate credit: enable UPI or interest, or use salary for credits",
                    HttpStatus.BAD_REQUEST.value());
        }
        String kind = credits.get(random.nextInt(credits.size()));
        return buildBoiKindTransaction(date, settings, balance, kind, accountNumber, periodFrom, periodTo,
                usedNarrations, () -> buildBoiCreditFromSettings(date, settings, balance, accountNumber, periodFrom,
                        periodTo, usedNarrations));
    }

    private Transaction buildBoiKindTransaction(LocalDate date, TransactionSettings settings, BigDecimal balance,
                                                String kind, String accountNumber, LocalDate periodFrom,
                                                LocalDate periodTo, Set<String> usedNarrations,
                                                java.util.function.Supplier<Transaction> creditFallback) {
        return switch (kind) {
            case "UPI_CR" -> {
                BoiNarrationGenerator.BoiTxnEntry entry = BoiNarrationGenerator.upiCredit(random);
                yield buildCredit(date, entry.narration(), entry.type(), balance,
                        randomActivityCreditAmount(), entry.reference());
            }
            case "INTEREST" -> {
                String narration = BoiNarrationGenerator.interestCredit(
                        accountNumber, periodFrom, periodTo, random);
                yield buildCredit(date, narration, "INT", balance, FinBoxAmountRules.interestCredit(random));
            }
            case "MERCHANT" -> {
                BoiNarrationGenerator.BoiTxnEntry entry = BoiNarrationGenerator.merchantQrDebit(random);
                yield buildDebitOrCreditFallback(date, entry.narration(), entry.type(), balance, creditFallback,
                        entry.reference());
            }
            case "ATM" -> {
                BoiNarrationGenerator.BoiTxnEntry entry = BoiNarrationGenerator.atmWithdrawal(random);
                yield buildDebitOrCreditFallback(date, entry.narration(), entry.type(), balance, creditFallback,
                        entry.reference());
            }
            case "EMI" -> {
                String narration = BoiNarrationGenerator.emiDebit(random);
                yield buildDebitOrCreditFallback(date, narration, "EMI", balance, creditFallback);
            }
            case "CHARGE" -> {
                String narration = BoiNarrationGenerator.unique(
                        () -> BoiNarrationGenerator.bankCharge(random), usedNarrations, random);
                yield buildDebitOrCreditFallback(date, narration, "CHG", balance, creditFallback);
            }
            default -> {
                BoiNarrationGenerator.BoiTxnEntry entry = BoiNarrationGenerator.upiDebit(random);
                yield buildDebitOrCreditFallback(date, entry.narration(), entry.type(), balance, creditFallback,
                        entry.reference());
            }
        };
    }

    private Transaction buildKotakDirected(LocalDate date, ActivityFlowPlanner.Direction direction,
                                           TransactionSettings settings, BigDecimal balance,
                                           Map<YearMonth, Integer> monthlyCredits,
                                           KotakNarrationGenerator kotakNarrations,
                                           KotakEcosystemContext ecosystem,
                                           Set<YearMonth> interestMonths) {
        YearMonth month = YearMonth.from(date);
        KotakChannel channel;
        if (direction == ActivityFlowPlanner.Direction.CREDIT
                && settings.isInterest()
                && !interestMonths.contains(month)) {
            interestMonths.add(month);
            channel = KotakChannel.INT_CREDIT;
        } else {
            channel = KotakChannelSelector.pick(random, direction, settings, month, interestMonths);
        }
        KotakNarrationGenerator.KotakTxnEntry entry =
                kotakNarrations.forChannel(channel, direction);
        BigDecimal amount = entry.fixedAmount() != null
                ? entry.fixedAmount()
                : kotakActivityAmount(channel, balance, direction);
        if (entry.direction() == KotakNarrationGenerator.Direction.CREDIT) {
            monthlyCredits.merge(month, 1, Integer::sum);
        }
        return buildKotakFromEntry(date, balance, entry, amount, monthlyCredits, kotakNarrations);
    }

    private BigDecimal kotakActivityAmount(KotakChannel channel, BigDecimal balance,
                                           ActivityFlowPlanner.Direction direction) {
        if (channel == KotakChannel.INT_CREDIT) {
            return KotakAcceptedAmountRules.interestAmount(random);
        }
        if (direction == ActivityFlowPlanner.Direction.DEBIT) {
            BigDecimal max = maxWithdrawable(balance, true);
            return KotakAcceptedAmountRules.capToMaxWithdrawable(
                    KotakAcceptedAmountRules.activityDebitAmount(random), max, random);
        }
        return KotakAcceptedAmountRules.activityCreditAmount(random);
    }

    private Map<YearMonth, Integer> monthlyCredits(YearMonth month) {
        return new HashMap<>(Map.of(month, 0));
    }

    private Transaction buildKotakDebitOrCredit(LocalDate date, KotakNarrationGenerator.KotakTxnEntry entry,
                                                BigDecimal balance, BigDecimal amount,
                                                KotakNarrationGenerator kotakNarrations,
                                                Map<YearMonth, Integer> monthlyCredits) {
        if (!canApplyStandardDebit(balance, maxWithdrawable(balance, true), true)) {
            KotakNarrationGenerator.KotakTxnEntry credit =
                    kotakNarrations.forChannel(KotakChannel.UPI_CREDIT, ActivityFlowPlanner.Direction.CREDIT);
            return buildKotakFromEntry(date, balance, credit,
                    KotakAcceptedAmountRules.activityCreditAmount(random), monthlyCredits, kotakNarrations);
        }
        return buildKotakFromEntry(date, balance, entry, amount, monthlyCredits, kotakNarrations);
    }

    private Transaction buildSbiDirected(LocalDate date, ActivityFlowPlanner.Direction direction,
                                       TransactionSettings settings, BigDecimal balance, String bankCode,
                                       KotakEcosystemContext ecosystem, Set<YearMonth> emiMonths) {
        if (direction == ActivityFlowPlanner.Direction.CREDIT) {
            return buildSbiCredit(date, settings, balance, bankCode, ecosystem);
        }
        return buildSbiDebit(date, settings, balance, bankCode, ecosystem, emiMonths);
    }

    private Transaction buildSbiCredit(LocalDate date, TransactionSettings settings, BigDecimal balance,
                                       String bankCode, KotakEcosystemContext ecosystem) {
        int pick = random.nextInt(100);
        if (pick < 15 && settings.isInterest()) {
            SbiNarrationGenerator.SbiTxnEntry entry = SbiNarrationGenerator.interestCredit(random);
            return buildCredit(date, entry.narration(), entry.type(), balance,
                    HighValueAmountRules.interestCredit(random), entry.reference());
        }
        String person = ecosystem.nextCounterparty();
        SbiNarrationGenerator.SbiTxnEntry entry = SbiNarrationGenerator.incomingCredit(random, person);
        return buildCredit(date, entry.narration(), entry.type(), balance,
                HighValueAmountRules.clusteredCredit(random), entry.reference());
    }

    private Transaction buildSbiDebit(LocalDate date, TransactionSettings settings, BigDecimal balance,
                                      String bankCode, KotakEcosystemContext ecosystem, Set<YearMonth> emiMonths) {
        YearMonth month = YearMonth.from(date);
        if (settings.isEmi() && !emiMonths.contains(month) && random.nextInt(100) < 20) {
            emiMonths.add(month);
            SbiNarrationGenerator.SbiTxnEntry entry = SbiNarrationGenerator.emiDebit(random);
            return buildHighValueDebitOrCreditFallback(date, entry.narration(), entry.type(), balance,
                    () -> buildSbiCredit(date, settings, balance, bankCode, ecosystem), entry.reference());
        }
        if (settings.isAtm() && random.nextInt(100) < 15) {
            SbiNarrationGenerator.SbiTxnEntry entry = SbiNarrationGenerator.atmWithdrawal(random);
            return buildHighValueDebitOrCreditFallback(date, entry.narration(), entry.type(), balance,
                    () -> buildSbiCredit(date, settings, balance, bankCode, ecosystem), entry.reference());
        }
        String party = random.nextBoolean() ? ecosystem.nextCounterparty() : ecosystem.nextMerchant();
        SbiNarrationGenerator.SbiTxnEntry entry = SbiNarrationGenerator.upiDebit(random, party);
        BigDecimal amount = highValueDebitAmountForFloor(balance);
        if (amount == null) {
            return buildSbiCredit(date, settings, balance, bankCode, ecosystem);
        }
        return buildHighValueDebit(date, entry.narration(), entry.type(), balance, amount, entry.reference());
    }

    private Transaction buildHighValueDebit(LocalDate date, String narration, String type,
                                            BigDecimal balance, BigDecimal amount) {
        return buildHighValueDebit(date, narration, type, balance, amount, null);
    }

    private Transaction buildHighValueDebit(LocalDate date, String narration, String type,
                                            BigDecimal balance, BigDecimal amount, String reference) {
        balance = scale(balance);
        amount = scale(amount);
        BigDecimal nextBalance = scale(balance.subtract(amount));
        assertPositiveRunningBalance(nextBalance);
        return Transaction.builder()
                .date(date)
                .narration(narration)
                .reference(reference != null ? reference : defaultReference())
                .type(type)
                .debit(amount)
                .credit(ZERO)
                .balance(nextBalance)
                .build();
    }

    private Transaction buildHighValueDebitOrCreditFallback(LocalDate date, String narration, String type,
                                                            BigDecimal balance,
                                                            java.util.function.Supplier<Transaction> creditFallback) {
        return buildHighValueDebitOrCreditFallback(date, narration, type, balance, creditFallback, null);
    }

    private Transaction buildHighValueDebitOrCreditFallback(LocalDate date, String narration, String type,
                                                            BigDecimal balance,
                                                            java.util.function.Supplier<Transaction> creditFallback,
                                                            String reference) {
        BigDecimal debitAmount = highValueDebitAmountForFloor(balance);
        if (debitAmount == null) {
            return creditFallback.get();
        }
        return buildHighValueDebit(date, narration, type, balance, debitAmount, reference);
    }

    private BigDecimal highValueDebitAmount(BigDecimal balance) {
        BigDecimal max = maxWithdrawable(balance, true);
        if (max.compareTo(KotakAcceptedAmountRules.ACTIVITY_MIN) < 0) {
            return null;
        }
        BigDecimal amount = KotakAcceptedAmountRules.capToMaxWithdrawable(
                KotakAcceptedAmountRules.activityDebitAmount(random), max, random);
        amount = enforceMinimumActivityDebit(amount, max);
        return amount.compareTo(ZERO) > 0 ? amount : null;
    }

    private BigDecimal highValueDebitAmountForFloor(BigDecimal balance) {
        BigDecimal max = maxWithdrawable(balance, false);
        if (max.compareTo(new BigDecimal(DEBIT_MIN_RUPEES)) < 0) {
            return null;
        }
        return HighValueAmountRules.capToMaxWithdrawable(HighValueAmountRules.clusteredDebit(random), max);
    }

    private Transaction buildKotakFromEntry(LocalDate date, BigDecimal balance,
                                            KotakNarrationGenerator.KotakTxnEntry entry, BigDecimal amount,
                                            Map<YearMonth, Integer> monthlyCredits,
                                            KotakNarrationGenerator kotakNarrations) {
        if (BankRemarkGenerator.isBannedEmployerCreditText(entry.narration())) {
            entry = kotakNarrations.forChannel(KotakChannel.UPI_CREDIT);
        }
        if (entry.direction() == KotakNarrationGenerator.Direction.CREDIT) {
            if (entry.fixedAmount() != null) {
                amount = entry.fixedAmount();
            }
            return buildCredit(date, entry.narration(), entry.type(), balance, scale(amount), entry.reference());
        }
        if (amount == null) {
            amount = highValueDebitAmount(balance);
        }
        amount = amount != null ? clampKotakDebitAmount(balance, amount) : highValueDebitAmount(balance);
        if (amount == null || amount.compareTo(ZERO) <= 0) {
            KotakNarrationGenerator.KotakTxnEntry fallback =
                    kotakNarrations.forChannel(KotakChannel.UPI_CREDIT);
            YearMonth month = YearMonth.from(date);
            monthlyCredits.merge(month, 1, Integer::sum);
            return buildCredit(date, fallback.narration(), fallback.type(), balance,
                    KotakAcceptedAmountRules.activityCreditAmount(random), fallback.reference());
        }
        return buildKotakDebit(date, entry.narration(), entry.type(), balance, amount, entry.reference());
    }

    private Transaction buildKotakDebitOrCreditFallback(LocalDate date, String narration, String type,
                                                          BigDecimal balance,
                                                          java.util.function.Supplier<Transaction> creditFallback,
                                                          String reference) {
        FinBoxAmountRules.Merchant merchant = KotakNarrationGenerator.merchantFromNarration(narration);
        BigDecimal debitAmount = randomKotakDebitAmount(balance, merchant);
        if (debitAmount == null) {
            return creditFallback.get();
        }
        return buildKotakDebit(date, narration, type, balance, debitAmount, reference);
    }

    private Transaction buildKotakDebit(LocalDate date, String narration, String type, BigDecimal balance,
                                        BigDecimal amount, String reference) {
        balance = scale(balance);
        amount = scale(amount);
        BigDecimal nextBalance = scale(balance.subtract(amount));
        assertPositiveRunningBalance(nextBalance, true);
        return Transaction.builder()
                .date(date)
                .narration(narration)
                .reference(reference != null ? reference : defaultReference())
                .type(type)
                .debit(amount)
                .credit(ZERO)
                .balance(nextBalance)
                .build();
    }

    private LocalDate jitterSalaryDate(LocalDate salaryDate, LocalDate from, LocalDate to) {
        int jitter = random.nextInt(5) - 2;
        LocalDate shifted = salaryDate.plusDays(jitter);
        if (shifted.isBefore(from)) {
            return from;
        }
        if (shifted.isAfter(to)) {
            return to;
        }
        return shifted;
    }

    private String randomKotakInterestPeriod() {
        int startDay = 1 + random.nextInt(28);
        int endDay = startDay + 30 + random.nextInt(120);
        return String.format("%02d-%02d-2025 to %02d-%02d-2025",
                startDay, random.nextInt(12) + 1, Math.min(endDay, 28), random.nextInt(12) + 1);
    }

    private String deriveLinkedSweepAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "5094491610";
        }
        return accountNumber.substring(0, Math.min(10, accountNumber.length()));
    }

    private String nullToEmptyAccount(String accountNumber) {
        return accountNumber == null || accountNumber.isBlank() ? "5251182222" : accountNumber;
    }

    private Transaction buildGenericTransaction(LocalDate date, TransactionSettings settings, BigDecimal balance,
                                              String bankCode) {
        if (!canWithdrawStandardDebit(balance)) {
            return buildGenericCredit(date, settings, balance, bankCode);
        }

        List<String> types = genericDebitKinds(settings);
        if (types.isEmpty()) {
            return buildGenericCredit(date, settings, balance, bankCode);
        }

        String type = types.get(random.nextInt(types.size()));
        java.util.function.Supplier<Transaction> creditFallback =
                () -> buildGenericCredit(date, settings, balance, bankCode);
        return switch (type) {
            case "UPI" -> {
                if (settings.isUpi() && shouldPreferCredit(balance)) {
                    yield buildGenericUpiCredit(date, balance, bankCode);
                }
                String narration = BankRemarkGenerator.retailDebit(random, bankCode);
                yield buildDebitOrCreditFallback(date, narration, "UPI", balance, creditFallback);
            }
            case "ATM" -> buildDebitOrCreditFallback(date,
                    BankRemarkGenerator.atmWithdrawal(random, bankCode), "ATM", balance,
                    creditFallback);
            case "EMI" -> buildDebitOrCreditFallback(date,
                    BankRemarkGenerator.emiDebit(random), "EMI", balance,
                    creditFallback);
            default -> buildGenericCredit(date, settings, balance, bankCode);
        };
    }

    private List<String> genericDebitKinds(TransactionSettings settings) {
        List<String> types = new ArrayList<>();
        if (settings.isUpi()) {
            types.add("UPI");
        }
        if (settings.isAtm()) {
            types.add("ATM");
        }
        if (settings.isEmi()) {
            types.add("EMI");
        }
        return types;
    }

    private List<String> genericCreditKinds(TransactionSettings settings) {
        List<String> types = new ArrayList<>();
        if (settings.isUpi()) {
            types.add("UPI");
        }
        if (settings.isInterest()) {
            types.add("INTEREST");
        }
        return types;
    }

    private Transaction buildGenericCredit(LocalDate date, TransactionSettings settings, BigDecimal balance,
                                           String bankCode) {
        List<String> types = genericCreditKinds(settings);
        if (types.isEmpty()) {
            throw new ApiException(
                    "Cannot generate credit: enable UPI or interest, or use salary",
                    HttpStatus.BAD_REQUEST.value());
        }
        String type = types.get(random.nextInt(types.size()));
        if ("UPI".equals(type)) {
            return buildGenericUpiCredit(date, balance, bankCode);
        }
        return buildCredit(date, BankRemarkGenerator.interestCredit(random), "INT", balance,
                FinBoxAmountRules.interestCredit(random));
    }

    private Transaction buildGenericUpiCredit(LocalDate date, BigDecimal balance, String bankCode) {
        String narration = BankRemarkGenerator.smallIncomingCredit(random, bankCode);
        return buildCredit(date, narration, "UPI", balance, randomActivityCreditAmount());
    }

    /**
     * Recomputes running balances in chronological order and caps any debit that would overdraw.
     * Debits that cannot respect the balance floor are converted to credits so running balance
     * never drops below {@link GeneratedStatementRules#MIN_RUNNING_BALANCE} (including when opening balance is low).
     */
    private void recalculateRunningBalances(List<Transaction> transactions, BigDecimal openingBalance,
                                            boolean kotak, KotakNarrationGenerator kotakNarrations) {
        BigDecimal running = scale(openingBalance);
        for (Transaction txn : transactions) {
            BigDecimal credit = positiveAmount(txn.getCredit());
            BigDecimal debit = positiveAmount(txn.getDebit());

            if (debit.compareTo(ZERO) > 0) {
                BigDecimal maxDebit = maxWithdrawable(running, kotak);
                if (!canApplyStandardDebit(running, maxDebit, kotak)) {
                    applyCreditInsteadOfDebit(txn, running, kotak, kotakNarrations);
                    debit = ZERO;
                    credit = positiveAmount(txn.getCredit());
                } else if (debit.compareTo(maxDebit) > 0) {
                    BigDecimal capped = kotak
                            ? highValueDebitAmount(running)
                            : randomDebitAmount(running);
                    if (capped != null) {
                        debit = kotak ? enforceMinimumActivityDebit(capped, maxDebit) : capped;
                    } else if (maxDebit.compareTo(kotak ? new BigDecimal("1.00") : new BigDecimal(DEBIT_MIN_RUPEES)) >= 0) {
                        debit = kotak ? enforceMinimumActivityDebit(maxDebit, maxDebit) : maxDebit;
                    } else {
                        applyCreditInsteadOfDebit(txn, running, kotak, kotakNarrations);
                        debit = ZERO;
                        credit = positiveAmount(txn.getCredit());
                    }
                    if (debit.compareTo(ZERO) > 0) {
                        txn.setDebit(debit);
                        txn.setCredit(ZERO);
                        credit = ZERO;
                    }
                }
            }

            BigDecimal minBalance = minRunningBalance(kotak);
            if (debit.compareTo(ZERO) <= 0 && credit.compareTo(ZERO) <= 0
                    && running.compareTo(minBalance) < 0) {
                applyCreditInsteadOfDebit(txn, running, kotak, kotakNarrations);
                credit = positiveAmount(txn.getCredit());
            }

            if (credit.compareTo(ZERO) > 0 && running.compareTo(minBalance) < 0) {
                credit = ensureCreditLiftsToFloor(running, credit, kotak);
                txn.setCredit(credit);
            }

            if (kotak) {
                if (!"SAL".equals(txn.getType())) {
                    applyKotakCreditCap(txn, running);
                    credit = positiveAmount(txn.getCredit());
                }
                applyKotakDebitAdjust(txn, running);
                debit = positiveAmount(txn.getDebit());
            }

            running = scale(running.add(positiveAmount(txn.getCredit())).subtract(positiveAmount(txn.getDebit())));
            assertPositiveRunningBalance(running, kotak);
            txn.setBalance(running);
        }
    }

    private BigDecimal minRunningBalance(boolean kotak) {
        return kotak ? KotakBalanceRules.MIN : GeneratedStatementRules.MIN_RUNNING_BALANCE;
    }

    /** True when a debit can be applied without breaching the bank floor. */
    private boolean canApplyStandardDebit(BigDecimal running, BigDecimal maxDebit, boolean kotak) {
        if (maxDebit.compareTo(ZERO) <= 0) {
            return false;
        }
        if (running.compareTo(minRunningBalance(kotak)) < 0) {
            return false;
        }
        return maxDebit.compareTo(new BigDecimal(DEBIT_MIN_RUPEES)) >= 0;
    }

    private boolean canWithdrawStandardDebit(BigDecimal balance) {
        return canApplyStandardDebit(balance, maxWithdrawable(balance, false), false);
    }

    /**
     * When a debit cannot be applied without dropping below the floor, convert the row to a credit
     * large enough to restore running balance to at least the configured floor.
     */
    private void applyCreditInsteadOfDebit(Transaction txn, BigDecimal running, boolean kotak,
                                           KotakNarrationGenerator kotakNarrations) {
        txn.setDebit(ZERO);
        BigDecimal minBalance = minRunningBalance(kotak);
        BigDecimal liftNeeded = scale(minBalance.subtract(running));
        BigDecimal amount;
        if (kotak) {
            amount = liftNeeded.compareTo(ZERO) > 0
                    ? liftNeeded
                    : KotakAcceptedAmountRules.activityCreditAmount(random);
            applyKotakCreditCap(txn, running, amount);
            amount = positiveAmount(txn.getCredit());
            KotakNarrationGenerator.KotakTxnEntry entry =
                    kotakNarrations.forChannel(KotakChannel.UPI_CREDIT);
            txn.setNarration(entry.narration());
            txn.setReference(entry.reference());
            txn.setType(entry.type());
        } else {
            amount = GeneratedStatementRules.activityCredit(random);
            if (running.add(amount).compareTo(minBalance) < 0) {
                amount = scale(minBalance.subtract(running));
            }
            if (amount.compareTo(GeneratedStatementRules.ACTIVITY_CREDIT_MIN_BD) < 0
                    && liftNeeded.compareTo(GeneratedStatementRules.ACTIVITY_CREDIT_MIN_BD) > 0) {
                amount = GeneratedStatementRules.ACTIVITY_CREDIT_MIN_BD;
            }
        }
        if (!kotak) {
            txn.setCredit(amount);
        }
        if ("ATM".equals(txn.getType()) || "EMI".equals(txn.getType()) || "CHG".equals(txn.getType())) {
            txn.setType("UPI");
        }
    }

    /** When running is below the floor, credit must lift balance to at least the configured minimum. */
    private BigDecimal ensureCreditLiftsToFloor(BigDecimal running, BigDecimal credit, boolean kotak) {
        BigDecimal minBalance = minRunningBalance(kotak);
        BigDecimal after = scale(running.add(credit));
        if (after.compareTo(minBalance) >= 0) {
            return kotak ? capKotakCredit(running, credit) : credit;
        }
        BigDecimal liftNeeded = scale(minBalance.subtract(running));
        if (kotak) {
            BigDecimal lifted = liftNeeded.compareTo(ZERO) > 0
                    ? liftNeeded
                    : KotakAcceptedAmountRules.activityCreditAmount(random);
            return capKotakCredit(running, lifted);
        }
        BigDecimal lifted = GeneratedStatementRules.activityCredit(random);
        if (running.add(lifted).compareTo(minBalance) < 0) {
            lifted = scale(minBalance.subtract(running));
        }
        return lifted;
    }

    private BigDecimal applyToBalance(BigDecimal balance, Transaction txn) {
        BigDecimal credit = positiveAmount(txn.getCredit());
        BigDecimal debit = positiveAmount(txn.getDebit());
        return scale(balance.add(credit).subtract(debit));
    }

    private Transaction buildDebitOrCreditFallback(LocalDate date, String narration, String type,
                                                     BigDecimal balance,
                                                     java.util.function.Supplier<Transaction> creditFallback) {
        return buildDebitOrCreditFallback(date, narration, type, balance, creditFallback, (String) null);
    }

    private Transaction buildDebitOrCreditFallback(LocalDate date, String narration, String type,
                                                     BigDecimal balance,
                                                     java.util.function.Supplier<Transaction> creditFallback,
                                                     int debitMin, int debitMax) {
        return buildDebitOrCreditFallback(date, narration, type, balance, creditFallback, (String) null);
    }

    private Transaction buildDebitOrCreditFallback(LocalDate date, String narration, String type,
                                                     BigDecimal balance,
                                                     java.util.function.Supplier<Transaction> creditFallback,
                                                     String reference) {
        BigDecimal debitAmount = randomDebitAmount(balance);
        if (debitAmount == null) {
            return creditFallback.get();
        }
        return buildDebit(date, narration, type, balance, debitAmount, reference);
    }

    private Transaction buildCredit(LocalDate date, String narration, String type, BigDecimal balance, BigDecimal amount) {
        return buildCredit(date, narration, type, balance, amount, null);
    }

    private Transaction buildCredit(LocalDate date, String narration, String type, BigDecimal balance, BigDecimal amount,
                                  String reference) {
        amount = scale(amount);
        BigDecimal nextBalance = scale(balance.add(amount));
        return Transaction.builder()
                .date(date)
                .narration(narration)
                .reference(reference != null ? reference : defaultReference())
                .type(type)
                .credit(amount)
                .debit(ZERO)
                .balance(nextBalance)
                .build();
    }

    private Transaction buildDebit(LocalDate date, String narration, String type, BigDecimal balance, BigDecimal amount) {
        return buildDebit(date, narration, type, balance, amount, null);
    }

    private Transaction buildDebit(LocalDate date, String narration, String type, BigDecimal balance, BigDecimal amount,
                                 String reference) {
        balance = scale(balance);
        amount = clampDebitAmount(balance, amount);
        BigDecimal nextBalance = scale(balance.subtract(amount));
        assertPositiveRunningBalance(nextBalance);
        return Transaction.builder()
                .date(date)
                .narration(narration)
                .reference(reference != null ? reference : defaultReference())
                .type(type)
                .debit(amount)
                .credit(ZERO)
                .balance(nextBalance)
                .build();
    }

    private String defaultReference() {
        return "REF" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private BigDecimal clampDebitAmount(BigDecimal balance, BigDecimal amount) {
        balance = scale(balance);
        BigDecimal maxDebit = maxWithdrawable(balance);
        if (maxDebit.compareTo(ZERO) <= 0) {
            return ZERO;
        }
        amount = RealisticBankAmount.capToBalance(random, maxDebit, amount);
        if (amount.compareTo(ZERO) <= 0) {
            BigDecimal fallback = randomDebitAmount(balance);
            return fallback != null ? fallback : RealisticBankAmount.generateUpTo(random, maxDebit);
        }
        return amount;
    }

    /** Largest debit allowed while keeping running balance at or above the bank floor. */
    private BigDecimal maxWithdrawable(BigDecimal balance, boolean kotak) {
        balance = scale(balance);
        BigDecimal max = scale(balance.subtract(minRunningBalance(kotak)));
        return max.compareTo(ZERO) > 0 ? max : ZERO;
    }

    /** Kotak credits cannot push running balance above {@link KotakBalanceRules#MAX}. */
    private BigDecimal capKotakCredit(BigDecimal running, BigDecimal credit) {
        credit = scale(credit);
        if (credit.compareTo(ZERO) <= 0) {
            return credit;
        }
        BigDecimal headroom = scale(KotakBalanceRules.MAX.subtract(running));
        if (headroom.compareTo(GeneratedStatementRules.ACTIVITY_CREDIT_MIN_BD) < 0) {
            return ZERO;
        }
        if (headroom.compareTo(ZERO) <= 0) {
            return ZERO;
        }
        BigDecimal capped = credit.compareTo(headroom) <= 0 ? credit : headroom;
        if (capped.compareTo(GeneratedStatementRules.ACTIVITY_CREDIT_MIN_BD) < 0) {
            return ZERO;
        }
        return capped;
    }

    private void applyKotakCreditCap(Transaction txn, BigDecimal running) {
        applyKotakCreditCap(txn, running, positiveAmount(txn.getCredit()));
    }

    private void applyKotakCreditCap(Transaction txn, BigDecimal running, BigDecimal intended) {
        intended = scale(intended);
        BigDecimal applied = capKotakCredit(running, intended);
        txn.setCredit(applied);
        if (applied.compareTo(ZERO) <= 0 && intended.compareTo(ZERO) > 0) {
            txn.setRenderCredit(intended);
        } else {
            txn.setRenderCredit(null);
        }
    }

    private void applyKotakDebitAdjust(Transaction txn, BigDecimal running) {
        BigDecimal intended = positiveAmount(txn.getDebit());
        BigDecimal applied = adjustKotakDebitWhenAboveCeiling(running, intended);
        txn.setDebit(applied);
        if (applied.compareTo(ZERO) <= 0 && intended.compareTo(ZERO) > 0) {
            txn.setRenderDebit(intended);
        } else {
            txn.setRenderDebit(null);
        }
    }

    /** When above Kotak ceiling, prefer larger debits to pull balance toward the band. */
    private BigDecimal adjustKotakDebitWhenAboveCeiling(BigDecimal running, BigDecimal debit) {
        debit = scale(debit);
        if (debit.compareTo(ZERO) <= 0) {
            return debit;
        }
        BigDecimal maxDebit = maxWithdrawable(running, true);
        if (running.compareTo(KotakBalanceRules.MAX) > 0) {
            BigDecimal pullToCeiling = scale(running.subtract(KotakBalanceRules.MAX));
            if (pullToCeiling.compareTo(maxDebit) > 0) {
                pullToCeiling = maxDebit;
            }
            if (debit.compareTo(pullToCeiling) < 0) {
                debit = pullToCeiling;
            }
        }
        return enforceMinimumActivityDebit(debit, maxDebit);
    }

    /** Activity debits must be at least ₹16k when non-zero, or zero if headroom is insufficient. */
    private BigDecimal enforceMinimumActivityDebit(BigDecimal debit, BigDecimal maxDebit) {
        debit = scale(debit);
        maxDebit = scale(maxDebit);
        if (debit.compareTo(ZERO) <= 0) {
            return ZERO;
        }
        if (debit.compareTo(GeneratedStatementRules.ACTIVITY_DEBIT_MIN_BD) >= 0) {
            return debit.compareTo(maxDebit) <= 0 ? debit : maxDebit;
        }
        if (maxDebit.compareTo(GeneratedStatementRules.ACTIVITY_DEBIT_MIN_BD) >= 0) {
            BigDecimal minDebit = GeneratedStatementRules.ACTIVITY_DEBIT_MIN_BD;
            return minDebit.compareTo(maxDebit) <= 0 ? minDebit : maxDebit;
        }
        return ZERO;
    }

    private BigDecimal maxWithdrawable(BigDecimal balance) {
        return maxWithdrawable(balance, false);
    }

    private BigDecimal randomDebitAmount(BigDecimal balance) {
        balance = scale(balance);
        BigDecimal maxDebit = maxWithdrawable(balance);
        if (maxDebit.compareTo(ZERO) <= 0) {
            return null;
        }
        FinBoxAmountRules.Merchant merchant = FinBoxAmountRules.randomDebitMerchant(random);
        BigDecimal candidate = FinBoxAmountRules.activityDebit(random, merchant);
        int cappedMax = Math.min(DEBIT_MAX_RUPEES, maxDebit.intValue());
        if (candidate.intValue() > cappedMax) {
            candidate = FinBoxAmountRules.activityDebit(random, FinBoxAmountRules.Merchant.GENERIC);
        }
        if (candidate.compareTo(maxDebit) > 0) {
            candidate = RealisticBankAmount.generateUpTo(random, maxDebit);
        }
        if (candidate.compareTo(ZERO) <= 0 || candidate.intValue() > DEBIT_MAX_RUPEES) {
            return null;
        }
        return candidate;
    }

    private BigDecimal randomActivityCreditAmount() {
        return FinBoxAmountRules.activityCredit(random);
    }

    private BigDecimal randomKotakCreditAmount(boolean ignoredAllowLarge) {
        return KotakAcceptedAmountRules.activityCreditAmount(random);
    }

    private BigDecimal randomKotakDebitAmount(BigDecimal balance, FinBoxAmountRules.Merchant merchant) {
        return highValueDebitAmount(balance);
    }

    private BigDecimal randomKotakDebitAmount(BigDecimal balance) {
        return randomKotakDebitAmount(balance, FinBoxAmountRules.randomDebitMerchant(random));
    }

    private BigDecimal clampKotakDebitAmount(BigDecimal balance, BigDecimal amount) {
        balance = scale(balance);
        BigDecimal maxDebit = maxWithdrawable(balance, true);
        if (maxDebit.compareTo(ZERO) <= 0) {
            return ZERO;
        }
        amount = KotakAcceptedAmountRules.capToMaxWithdrawable(scale(amount), maxDebit, random);
        amount = enforceMinimumActivityDebit(amount, maxDebit);
        if (amount.compareTo(ZERO) <= 0) {
            BigDecimal fallback = highValueDebitAmount(balance);
            return fallback != null ? fallback : ZERO;
        }
        return amount;
    }

    private boolean shouldPreferCredit(BigDecimal balance) {
        if (!canWithdrawStandardDebit(balance)) {
            return true;
        }
        if (balance.compareTo(GeneratedStatementRules.MIN_RUNNING_BALANCE.add(new BigDecimal("20000.00"))) <= 0) {
            return random.nextInt(100) < 75;
        }
        return random.nextBoolean();
    }

    private BigDecimal positiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(ZERO) <= 0) {
            return ZERO;
        }
        return scale(amount);
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private void assertPositiveRunningBalance(BigDecimal balance, boolean kotak) {
        if (balance.compareTo(minRunningBalance(kotak)) < 0) {
            throw new IllegalStateException(
                    "Running balance must stay at or above " + minRunningBalance(kotak) + ": " + balance);
        }
    }

    private void assertPositiveRunningBalance(BigDecimal balance) {
        assertPositiveRunningBalance(balance, false);
    }

    private String randomDigits(int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append(random.nextInt(10));
        return sb.toString();
    }
}
