package com.bankstatement.service.pdf;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

/** Font Awesome Solid icons for Kotak contact footer (embedded, FinBox-safe text layer). */
final class KotakPdfIcons {

    static final String PHONE = "\uf095";
    /** Bank / branch icon (Font Awesome solid). */
    static final String BRANCH = "\uf19c";

    private static PdfFont iconFont;

    private KotakPdfIcons() {}

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
