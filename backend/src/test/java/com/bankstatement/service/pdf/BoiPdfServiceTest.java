package com.bankstatement.service.pdf;

import com.bankstatement.entity.*;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfVersion;
import com.itextpdf.kernel.pdf.ReaderProperties;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BoiPdfServiceTest {

    @Test
    void supportsBoiBankCode() {
        BoiPdfService service = new BoiPdfService();
        assertTrue(service.supports("BOI"));
        assertTrue(service.supports("boi"));
        assertFalse(service.supports("SBI"));
        assertFalse(service.supports("KOTAK"));
    }

    @Test
    void generatesPdf() {
        byte[] pdf = new BoiPdfService().generate(sampleStatement(), false);
        assertTrue(pdf.length > 1000);
    }

    @Test
    void previewPdfIsUnencrypted() throws Exception {
        Statement statement = sampleStatement();
        byte[] pdf = new BoiPdfService().generate(statement, false);
        assertTrue(new String(pdf, 0, Math.min(pdf.length, 16)).startsWith("%PDF-1.7"));

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf));
             PdfDocument doc = new PdfDocument(reader)) {
            assertEquals(PdfVersion.PDF_1_7, doc.getPdfVersion());
            assertEquals(BoiItext5Finalizer.PRODUCER, doc.getDocumentInfo().getProducer());
            assertTrue(isBlank(doc.getDocumentInfo().getTitle()));
            assertTrue(isBlank(doc.getDocumentInfo().getAuthor()));
            assertTrue(isBlank(doc.getDocumentInfo().getSubject()));
            assertTrue(doc.getNumberOfPages() >= 1);
        }
    }

    @Test
    void downloadPdfIsPasswordProtected() throws Exception {
        Statement statement = sampleStatement();
        byte[] pdf = new BoiPdfService().generateForDownload(statement);
        String password = BoiPdfPassword.generate(statement.getCustomerDetails());
        assertTrue(new String(pdf, 0, Math.min(pdf.length, 16)).startsWith("%PDF-1.7"));

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf))) {
            reader.setUnethicalReading(true);
            assertThrows(Exception.class, () -> new PdfDocument(reader));
        }

        ReaderProperties props = new ReaderProperties().setPassword(password.getBytes());
        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf), props);
             PdfDocument doc = new PdfDocument(reader)) {
            assertEquals(PdfVersion.PDF_1_7, doc.getPdfVersion());
            assertEquals(BoiItext5Finalizer.PRODUCER, doc.getDocumentInfo().getProducer());
            assertTrue(isBlank(doc.getDocumentInfo().getTitle()));
            assertTrue(isBlank(doc.getDocumentInfo().getAuthor()));
            assertTrue(isBlank(doc.getDocumentInfo().getSubject()));
            assertTrue(doc.getNumberOfPages() >= 1);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Test
    void buildDownloadFilename() {
        String name = new BoiPdfService().buildDownloadFilename(sampleStatement());
        assertEquals("BOI_Statement_995610110012688.pdf", name);
    }

    @Test
    void accountHolderNameExtractedCharForCharOnSingleLine() throws Exception {
        Statement statement = sampleStatement();
        statement.getCustomerDetails().setCustomerName("  AJAY\u00A0 SINGH   SO  SIDDNATH. ");
        String expected = "AJAY SINGH SO SIDDNATH";

        byte[] pdf = new BoiPdfService().generate(statement, false);
        String pageText;
        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf));
             PdfDocument doc = new PdfDocument(reader)) {
            pageText = PdfTextExtractor.getTextFromPage(doc.getPage(1));
        }

        assertTrue(
                pageText.contains(BoiAccountHolderNameFormatter.labelWithValue(expected)),
                () -> "Expected single-line label+value in text layer. Actual page text:\n" + pageText);
        assertEquals(expected, BoiAccountHolderNameFormatter.parseFromExtractedText(pageText));
    }

    private Statement sampleStatement() {
        return Statement.builder()
                .bankCode("BOI")
                .customerDetails(CustomerDetails.builder()
                        .customerName("PAVAN SINGH SO BAPULAL")
                        .customerId("123456789")
                        .dateOfBirth(LocalDate.of(1998, 1, 24))
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
                                .narration("UPI/306733838703/DR/ASHISH/BKID/825191188/Sent u")
                                .debit(new BigDecimal("1500.00"))
                                .balance(new BigDecimal("4797.59"))
                                .build(),
                        Transaction.builder()
                                .date(LocalDate.of(2026, 5, 20))
                                .narration("UPI/291835545426/CR/AMRIT/UBIN/939915023/Paymen")
                                .credit(new BigDecimal("8200.00"))
                                .balance(new BigDecimal("16847.59"))
                                .build()
                ))
                .build();
    }
}
