package com.bankstatement.dto;

import java.util.List;

public record NarrationSuggestionResponse(
        List<String> suggestions,
        String message
) {}
