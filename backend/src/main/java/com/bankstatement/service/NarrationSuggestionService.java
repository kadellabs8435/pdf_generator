package com.bankstatement.service;

import com.bankstatement.dto.NarrationSuggestionResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Phase 5 stub — plug in OpenAI or local LLM implementation later.
 */
@Service
public class NarrationSuggestionService {

    public NarrationSuggestionResponse suggest(String context, String transactionType) {
        List<String> suggestions = switch (transactionType != null ? transactionType.toUpperCase() : "UPI") {
            case "SALARY" -> List.of("SALARY CREDIT - EMPLOYER", "NEFT-SALARY-CREDIT", "IMPS/SALARY/CREDIT");
            case "ATM" -> List.of("ATM WDL - SELF", "ATM CASH WITHDRAWAL", "NWD/ATM/WITHDRAWAL");
            case "EMI" -> List.of("AUTO EMI - HOME LOAN", "ECS/EMI/DEBIT", "LOAN EMI PAYMENT");
            case "INTEREST" -> List.of("SB INT CREDIT", "INTEREST CREDIT QTR", "FD INT CREDIT");
            default -> List.of("UPI/PAYMENT/" + (context != null ? context : "MERCHANT"),
                    "IMPS/P2A/TRANSFER", "NEFT/TRANSFER/CREDIT");
        };

        return new NarrationSuggestionResponse(
                suggestions,
                "Phase 5 stub — AI narration service not yet connected."
        );
    }
}
