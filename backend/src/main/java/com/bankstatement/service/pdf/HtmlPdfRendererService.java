package com.bankstatement.service.pdf;

import com.bankstatement.exception.ApiException;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

/**
 * Converts frontend-rendered HTML into PDF bytes (OpenHTMLtoPDF).
 * Used during layout migration — does not replace bank-specific post-processors yet.
 */
@Service
@Slf4j
public class HtmlPdfRendererService {

    public byte[] renderHtml(String html) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            log.error("HTML to PDF conversion failed", e);
            throw new ApiException("Layout PDF conversion failed: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
}
