package com.bankstatement.service.transaction.llm;

import com.bankstatement.entity.Statement;
import com.bankstatement.entity.Transaction;
import com.bankstatement.entity.TransactionSettings;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TransactionValidationService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    public List<Transaction> validateAndConvert(Statement statement, List<LlmTransactionItem> items) {
        if (statement == null || statement.getTransactionSettings() == null) {
            throw new IllegalArgumentException("Statement and transaction settings are required");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("At least one transaction item is required");
        }

        TransactionSettings settings = statement.getTransactionSettings();
        List<LlmTransactionItem> deduped = deduplicate(items);

        List<LlmTransactionItem> salaryItems = new ArrayList<>();
        List<LlmTransactionItem> activityItems = new ArrayList<>();
        for (LlmTransactionItem item : deduped) {
            if (isSalaryItem(item)) {
                validateSalaryItem(item, settings);
                salaryItems.add(item);
            } else {
                activityItems.add(item);
            }
        }

        int min = Math.max(0, settings.getMinTransactions());
        int max = Math.max(min, settings.getMaxTransactions());
        if (activityItems.size() > max) {
            activityItems = new ArrayList<>(activityItems.subList(0, max));
        }
        if (activityItems.size() < min) {
            throw new IllegalArgumentException(
                    "Expected at least " + min + " activity transactions, got " + activityItems.size());
        }

        List<LlmTransactionItem> merged = new ArrayList<>(salaryItems.size() + activityItems.size());
        merged.addAll(salaryItems);
        merged.addAll(activityItems);
        merged.sort(Comparator
                .comparing((LlmTransactionItem item) -> LocalDate.parse(item.getDate()))
                .thenComparing(item -> isSalaryItem(item) ? 0 : 1)
                .thenComparing(item -> nullToEmpty(item.getDescription())));

        return buildWithRunningBalances(statement.getOpeningBalance(), merged);
    }

    private List<LlmTransactionItem> deduplicate(List<LlmTransactionItem> items) {
        Map<String, LlmTransactionItem> unique = new LinkedHashMap<>();
        for (LlmTransactionItem item : items) {
            unique.putIfAbsent(dedupeKey(item), item);
        }
        return new ArrayList<>(unique.values());
    }

    private String dedupeKey(LlmTransactionItem item) {
        return nullToEmpty(item.getDate()) + '|'
                + nullToEmpty(item.getType()) + '|'
                + scale(item.getAmount()) + '|'
                + nullToEmpty(item.getDescription()) + '|'
                + nullToEmpty(item.getChannel());
    }

    private boolean isSalaryItem(LlmTransactionItem item) {
        return "salary".equalsIgnoreCase(nullToEmpty(item.getChannel()));
    }

    private void validateSalaryItem(LlmTransactionItem item, TransactionSettings settings) {
        if (!"credit".equalsIgnoreCase(nullToEmpty(item.getType()))) {
            throw new IllegalArgumentException("Salary transaction must be a credit");
        }
        String company = settings.getSalaryCompanyName();
        if (company == null || company.isBlank()) {
            throw new IllegalArgumentException("Salary company name is not configured");
        }
        String description = nullToEmpty(item.getDescription());
        if (!description.toUpperCase(Locale.ROOT).contains(company.trim().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException(
                    "Salary description must contain configured company name: " + company.trim());
        }

        BigDecimal expected = scale(settings.getSalaryAmount());
        BigDecimal actual = scale(item.getAmount());
        if (actual.compareTo(expected) != 0) {
            throw new IllegalArgumentException(
                    "Salary amount must be exactly " + expected + ", got " + actual);
        }

        int salaryDay = settings.getSalaryDayOfMonth() != null ? settings.getSalaryDayOfMonth() : 1;
        LocalDate date = LocalDate.parse(item.getDate());
        if (date.getDayOfMonth() != salaryDay) {
            throw new IllegalArgumentException(
                    "Salary credit day must be " + salaryDay + ", got " + date.getDayOfMonth());
        }
    }

    private List<Transaction> buildWithRunningBalances(BigDecimal openingBalance,
                                                       List<LlmTransactionItem> items) {
        BigDecimal running = scale(openingBalance);
        List<Transaction> transactions = new ArrayList<>(items.size());

        for (LlmTransactionItem item : items) {
            BigDecimal amount = scale(item.getAmount());
            boolean credit = "credit".equalsIgnoreCase(nullToEmpty(item.getType()));
            boolean debit = "debit".equalsIgnoreCase(nullToEmpty(item.getType()));
            if (!credit && !debit) {
                throw new IllegalArgumentException("Transaction type must be credit or debit");
            }
            if (credit && debit) {
                throw new IllegalArgumentException("Transaction cannot be both credit and debit");
            }

            Transaction txn = Transaction.builder()
                    .date(LocalDate.parse(item.getDate()))
                    .narration(nullToEmpty(item.getDescription()))
                    .reference(item.getReference() != null ? item.getReference() : "")
                    .type(resolveType(item))
                    .credit(credit ? amount : ZERO)
                    .debit(debit ? amount : ZERO)
                    .build();

            running = credit ? running.add(amount) : running.subtract(amount);
            txn.setBalance(scale(running));
            transactions.add(txn);
        }
        return transactions;
    }

    private String resolveType(LlmTransactionItem item) {
        if (isSalaryItem(item)) {
            return "SAL";
        }
        String channel = nullToEmpty(item.getChannel()).toLowerCase(Locale.ROOT);
        return switch (channel) {
            case "upi" -> "UPI";
            case "atm" -> "ATM";
            case "emi" -> "EMI";
            case "interest" -> "INT";
            case "neft" -> "NEFT";
            case "imps" -> "IMPS";
            case "charge" -> "CHG";
            default -> channel.isBlank() ? "TXN" : channel.toUpperCase(Locale.ROOT);
        };
    }

    private static BigDecimal scale(BigDecimal amount) {
        if (amount == null) {
            return ZERO;
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
