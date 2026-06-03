package com.bankstatement.service.pdf;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

/** Embedded TrueType fonts with Identity-H encoding for bank PDF output. */
final class BankPdfFonts {

    private static final String REGULAR_PATH = "pdf/Arial.ttf";
    private static final String BOLD_PATH = "pdf/Arial-Bold.ttf";

    private static PdfFont regular;
    private static PdfFont bold;

    private BankPdfFonts() {}

    static PdfFont regular() {
        if (regular == null) {
            regular = load(REGULAR_PATH, StandardFonts.HELVETICA);
        }
        return regular;
    }

    static PdfFont bold() {
        if (bold == null) {
            bold = load(BOLD_PATH, StandardFonts.HELVETICA_BOLD);
        }
        return bold;
    }

    static void clearCache() {
        regular = null;
        bold = null;
    }

    private static PdfFont load(String classpath, String fallback) {
        try (InputStream in = new ClassPathResource(classpath).getInputStream()) {
            return PdfFontFactory.createFont(
                    in.readAllBytes(),
                    PdfEncodings.IDENTITY_H,
                    PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED,
                    true);
        } catch (Exception e) {
            try {
                return PdfFontFactory.createFont(fallback);
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to load PDF font: " + classpath, ex);
            }
        }
    }
}
