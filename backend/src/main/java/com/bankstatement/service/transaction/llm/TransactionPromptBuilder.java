package com.bankstatement.service.transaction.llm;

import com.bankstatement.entity.Statement;
import com.bankstatement.entity.StatementPeriod;
import com.bankstatement.entity.TransactionSettings;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TransactionPromptBuilder {

    public String buildSystemPrompt() {
        return """
                You generate realistic Indian bank statement transactions as JSON only.
                Output MUST be a valid JSON array with no markdown fences and no commentary.
                Never invent employer/company names — use only the company name provided in the user prompt.
                Amounts are INR with at most 2 decimal places (prefer whole rupees).
                Balance must never go negative when transactions are applied in chronological order.
                Use realistic Indian banking narrations matching the specified bank style.
                Do not use words TEST, DEMO, DUMMY, or PLACEHOLDER.
                """;
    }

    public String buildUserPrompt(Statement statement) {
        TransactionSettings s = statement.getTransactionSettings();
        StatementPeriod period = statement.getPeriod();
        String bank = statement.getBankCode() != null ? statement.getBankCode().toUpperCase() : "SBI";
        String narrationGuide = narrationGuideFor(bank);

        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate Indian banking transactions in JSON.\n\n");
        prompt.append("Context:\n");
        prompt.append("- bank: ").append(bank).append('\n');
        prompt.append("- openingBalance: ").append(statement.getOpeningBalance()).append('\n');
        prompt.append("- dateRange: ").append(period.getFromDate()).append(" to ").append(period.getToDate()).append('\n');
        prompt.append("- minTransactions (non-salary activity rows): ").append(s.getMinTransactions()).append('\n');
        prompt.append("- maxTransactions (non-salary activity rows): ").append(s.getMaxTransactions()).append('\n');
        prompt.append("- salaryEnabled: ").append(s.isSalary()).append('\n');
        if (s.isSalary() && hasConfiguredSalary(s)) {
            prompt.append("- companyName EXACT (must appear verbatim in every salary description): ")
                    .append(s.getSalaryCompanyName().trim()).append('\n');
            prompt.append("- salaryAmount EXACT: ").append(s.getSalaryAmount()).append('\n');
            prompt.append("- salaryCreditDay EXACT (day of month 1-28): ").append(s.getSalaryDayOfMonth()).append('\n');
            prompt.append("- include one salary credit on each month in the date range on salaryCreditDay\n");
        }
        prompt.append("- upiEnabled: ").append(s.isUpi()).append('\n');
        prompt.append("- atmEnabled: ").append(s.isAtm()).append('\n');
        prompt.append("- emiEnabled: ").append(s.isEmi()).append('\n');
        prompt.append("- interestEnabled: ").append(s.isInterest()).append('\n');
        prompt.append('\n').append(narrationGuide).append('\n');
        prompt.append("""
                
                Return JSON array only. Each object:
                {
                  "date": "YYYY-MM-DD",
                  "type": "credit" or "debit",
                  "amount": number,
                  "description": "realistic bank narration",
                  "channel": "salary|upi|atm|emi|interest|neft|imps|card|charge|transfer",
                  "reference": "optional ref id"
                }
                
                Rules:
                - Non-salary transaction count must be between minTransactions and maxTransactions inclusive.
                - Only use enabled channels (respect salary/upi/atm/emi/interest flags).
                - Salary rows: type=credit, channel=salary, amount=salaryAmount exactly, date on salaryCreditDay each month.
                - company name in salary description must match EXACTLY.
                - Vary narrations — avoid repeating identical descriptions.
                - Mix debit and credit naturally; debits only when balance allows.
                """);
        return prompt.toString();
    }

    private static String narrationGuideFor(String bank) {
        return switch (bank) {
            case "KOTAK" -> """
                    Kotak narration style examples:
                    - UPI/RAGHUVEER SINGH/847896737671/Payment from Ph
                    - Recd:IMPS/525766356457/ASHISH/KKBK/X2361/NA
                    - NEFT AXNGG27798263127 GOOGLE INDIA
                    - Int.Pd:5251182222:01-01-2026 to 31-03-2026
                    - POS/DMART/BHOPAL
                    """;
            case "BOI" -> """
                    BOI narration style examples:
                    - UPI/306733838703/DR/ASHISH/BKID/825191188
                    - TO TRANSFER
                    - NEFT/AXNFCN1234567890/BKID/VENDOR PAYMENT
                    - ATM WDL/BIORA SSI/MP
                    - SB INTEREST CREDIT
                    """;
            default -> """
                    SBI narration style examples:
                    - BY TRANSFER
                    - UPI/998276542190/SWIGGY/SBIN
                    - IMPS CR FROM RAMESH
                    - ATM CASH WDL/BHOPAL
                    - ECS/BAJAJ FINSERV EMI
                    """;
        };
    }

    private static boolean hasConfiguredSalary(TransactionSettings settings) {
        return settings.getSalaryCompanyName() != null
                && !settings.getSalaryCompanyName().isBlank()
                && settings.getSalaryAmount() != null
                && settings.getSalaryAmount().compareTo(BigDecimal.ZERO) > 0
                && settings.getSalaryDayOfMonth() != null
                && settings.getSalaryDayOfMonth() >= 1
                && settings.getSalaryDayOfMonth() <= 28;
    }
}
