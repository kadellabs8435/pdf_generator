package com.bankstatement.service.pdf;

import com.bankstatement.entity.*;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/** Writes a sample BOI PDF and prints exact text-layer extraction for FinBox audit. */
class BoiPdfTextDumpTest {

    @Test
    void dumpAccountHolderNameExtraction() throws Exception {
        Statement statement = Statement.builder()
                .bankCode("BOI")
                .customerDetails(CustomerDetails.builder()
                        .customerName("AJAY SINGH SO SIDDNATH")
                        .customerId("123456789")
                        .address("PADLI MAHARAJA")
                        .city("RAJGARH")
                        .pincode("465674")
                        .build())
                .accountDetails(AccountDetails.builder()
                        .accountNumber("995610110012688")
                        .branchName("BIAORA SSI")
                        .ifscCode("BKID0009956")
                        .build())
                .period(StatementPeriod.builder()
                        .fromDate(LocalDate.of(2025, 12, 1))
                        .toDate(LocalDate.of(2026, 5, 24))
                        .build())
                .openingBalance(new BigDecimal("5000.00"))
                .closingBalance(new BigDecimal("4797.59"))
                .transactions(List.of(
                        Transaction.builder()
                                .date(LocalDate.of(2026, 5, 24))
                                .narration("UPI test")
                                .debit(new BigDecimal("1500.00"))
                                .balance(new BigDecimal("4797.59"))
                                .build()))
                .build();

        byte[] pdf = new BoiPdfService().generate(statement, false);
        Path out = Path.of("target", "boi-audit-sample.pdf");
        Files.write(out, pdf);

        String pageText;
        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf));
             PdfDocument doc = new PdfDocument(reader)) {
            pageText = PdfTextExtractor.getTextFromPage(doc.getPage(1));
        }

        String parsed = BoiAccountHolderNameFormatter.parseFromExtractedText(pageText);
        System.out.println("=== BOI PDF audit sample ===");
        System.out.println("PDF written: " + out.toAbsolutePath());
        System.out.println("Rendered line: " + BoiAccountHolderNameFormatter.labelWithValue(
                BoiAccountHolderNameFormatter.forPdf(statement.getCustomerDetails().getCustomerName())));
        System.out.println("Extracted holder name (char-for-char): [" + parsed + "]");
        System.out.println("--- page text snippet ---");
        int idx = pageText.toLowerCase().indexOf("account holder");
        if (idx >= 0) {
            System.out.println(pageText.substring(Math.max(0, idx - 20), Math.min(pageText.length(), idx + 120)));
        } else {
            System.out.println(pageText.substring(0, Math.min(500, pageText.length())));
        }
    }
}
