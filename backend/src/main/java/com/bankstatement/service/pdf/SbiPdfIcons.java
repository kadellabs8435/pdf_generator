package com.bankstatement.service.pdf;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.PdfEncodings;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

/** Font Awesome Solid icons for SBI PDF layout. */
final class SbiPdfIcons {

    static final String USER = "\uf007";
    static final String ENVELOPE = "\uf0e0";
    static final String LOCATION = "\uf3c5";
    static final String CALENDAR = "\uf133";
    static final String WALLET = "\uf555";
    static final String CREDIT_CARD = "\uf09d";
    static final String BUILDING = "\uf1ad";
    static final String BANK = "\uf19c";
    static final String FILE = "\uf15c";
    static final String USER_PLUS = "\uf234";

    private static PdfFont iconFont;

    static PdfFont font() {
        if (iconFont == null) {
            iconFont = loadFont();
        }
        return iconFont;
    }

    private static PdfFont loadFont() {
        try (InputStream in = new ClassPathResource("pdf/fa-solid-900.ttf").getInputStream()) {
            byte[] bytes = in.readAllBytes();
            return PdfFontFactory.createFont(bytes, PdfEncodings.IDENTITY_H,
                    PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
        } catch (Exception e) {
            try {
                return PdfFontFactory.createFont(StandardFonts.HELVETICA);
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to load icon font", ex);
            }
        }
    }

    static void clearCache() {
        iconFont = null;
    }
}
