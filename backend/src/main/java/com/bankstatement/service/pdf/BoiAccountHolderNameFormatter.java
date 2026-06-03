package com.bankstatement.service.pdf;

import java.util.Locale;
import java.util.regex.Pattern;

/** Normalizes account holder names for BOI PDF text extraction (FinBox compatibility). */
final class BoiAccountHolderNameFormatter {

    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    private BoiAccountHolderNameFormatter() {}

    /**
     * Produces the exact string embedded in the PDF text layer:
     * trim, collapse whitespace, strip invisible chars, remove trailing dots.
     */
    static String forPdf(String raw) {
        if (raw == null) {
            return "";
        }
        String normalized = raw
                .replace('\u00A0', ' ')
                .replace('\u2007', ' ')
                .replace('\u202F', ' ')
                .replace('\u200B', ' ')
                .replace('\uFEFF', ' ')
                .trim();
        normalized = MULTI_SPACE.matcher(normalized).replaceAll(" ");
        while (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        return normalized;
    }

    /** Label + value as a single extractable line for FinBox parsers. */
    static String labelWithValue(String normalizedName) {
        return "Account holder name: " + normalizedName;
    }

    /** Parses holder name from full page text after PDF generation (tests / diagnostics). */
    static String parseFromExtractedText(String pageText) {
        if (pageText == null || pageText.isBlank()) {
            return "";
        }
        String flat = MULTI_SPACE.matcher(pageText.replace('\r', ' ').replace('\n', ' ')).replaceAll(" ");
        String marker = "account holder name:";
        int idx = flat.toLowerCase(Locale.ROOT).indexOf(marker);
        if (idx < 0) {
            return "";
        }
        String after = flat.substring(idx + marker.length()).trim();
        after = truncateAtMarker(after, "account holder address:");
        after = truncateAtMarker(after, "customer id:");
        return after.trim();
    }

    private static String truncateAtMarker(String text, String nextMarker) {
        int next = text.toLowerCase(Locale.ROOT).indexOf(nextMarker);
        if (next > 0) {
            return text.substring(0, next).trim();
        }
        return text;
    }
}
