package com.bankstatement.service.pdf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoiAccountHolderNameFormatterTest {

    @Test
    void forPdf_trimsCollapsesSpacesAndRemovesNbsp() {
        String raw = "  AJAY\u00A0 SINGH   SO  SIDDNATH  ";
        assertEquals("AJAY SINGH SO SIDDNATH", BoiAccountHolderNameFormatter.forPdf(raw));
    }

    @Test
    void forPdf_removesTrailingDot() {
        assertEquals("PAVAN SINGH SO BAPULAL", BoiAccountHolderNameFormatter.forPdf("PAVAN SINGH SO BAPULAL."));
    }

    @Test
    void forPdf_removesTrailingDotEvenAfterSoDoSuffix() {
        assertEquals("RAJ S/O KUMAR", BoiAccountHolderNameFormatter.forPdf("RAJ S/O KUMAR."));
    }

    @Test
    void labelWithValue_singleLineFormat() {
        assertEquals(
                "Account holder name: AJAY SINGH SO SIDDNATH",
                BoiAccountHolderNameFormatter.labelWithValue("AJAY SINGH SO SIDDNATH"));
    }

    @Test
    void parseFromExtractedText_readsNameAfterLabel() {
        String pageText = "Account holder name: PAVAN SINGH SO BAPULAL Account holder address: PADLI MAHARAJA"
                + " Customer ID: 123456789";
        assertEquals("PAVAN SINGH SO BAPULAL", BoiAccountHolderNameFormatter.parseFromExtractedText(pageText));
    }
}
