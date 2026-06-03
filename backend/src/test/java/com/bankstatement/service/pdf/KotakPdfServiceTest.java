package com.bankstatement.service.pdf;

import com.bankstatement.entity.*;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfVersion;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class KotakPdfServiceTest {

    private static final String REFERENCE_CREATION_DATE = KotakOpenPdfMetadata.REFERENCE_CREATION_DATE;

    @Test
    void generatesPdf() {
        byte[] pdf = new KotakPdfService().generate(sampleStatement(), false);
        assertTrue(pdf.length > 1000);
    }

    @Test
    void pdfStructureMatchesBankExport() throws Exception {
        byte[] pdf = new KotakPdfService().generate(sampleStatement(), false);
        assertTrue(new String(pdf, 0, Math.min(pdf.length, 16)).startsWith("%PDF-1.5"));

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf));
             PdfDocument doc = new PdfDocument(reader)) {
            assertEquals(PdfVersion.PDF_1_5, doc.getPdfVersion());

            assertEquals(KotakOpenPdfMetadata.OPENPDF_PRODUCER, doc.getDocumentInfo().getProducer());
            assertNull(doc.getDocumentInfo().getTitle());
            assertNull(doc.getDocumentInfo().getAuthor());
            assertNull(doc.getDocumentInfo().getCreator());
            assertNull(doc.getDocumentInfo().getSubject());
            assertNull(doc.getDocumentInfo().getMoreInfo(PdfName.ModDate.getValue()));

            String creationDate = doc.getDocumentInfo().getMoreInfo(PdfName.CreationDate.getValue());
            assertEquals(REFERENCE_CREATION_DATE, creationDate);

            for (int i = 1; i <= doc.getNumberOfPages(); i++) {
                var pageObj = doc.getPage(i).getPdfObject();
                assertNull(pageObj.get(PdfName.TrimBox));
                assertNotNull(pageObj.get(PdfName.Group));
                assertFalse(pageObj.get(PdfName.Contents).isArray(),
                        "page " + i + " should have single content stream");
            }
        }
    }

    @Test
    void finboxOptimizedDownloadPdf() throws Exception {
        byte[] pdf = new KotakPdfService().generateForDownload(sampleStatement());
        Files.write(Path.of("target/kotak-finbox-sample.pdf"), pdf);

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf));
             PdfDocument doc = new PdfDocument(reader)) {
            assertEquals(KotakOpenPdfMetadata.OPENPDF_PRODUCER, doc.getDocumentInfo().getProducer());
            assertEquals(REFERENCE_CREATION_DATE,
                    doc.getDocumentInfo().getMoreInfo(PdfName.CreationDate.getValue()));
            assertNull(doc.getDocumentInfo().getTitle());
            assertNull(doc.getDocumentInfo().getAuthor());
        }
    }

    @Test
    void infoDictionaryKeyOrderMatchesOpenPdfReference() throws Exception {
        byte[] pdf = new KotakPdfService().generate(sampleStatement(), false);
        String infoSnippet = extractInfoDictionarySnippet(pdf);
        int creationIdx = infoSnippet.indexOf("/CreationDate");
        int producerIdx = infoSnippet.indexOf("/Producer");
        assertTrue(creationIdx >= 0 && producerIdx >= 0, "Info dictionary missing expected keys");
        assertTrue(creationIdx < producerIdx, "CreationDate must appear before Producer in Info dict");
        assertTrue(infoSnippet.contains("/Producer (OpenPDF 2.0.3)"));
        assertTrue(infoSnippet.contains("/CreationDate (D:20260521163150Z)"));
    }

    @Test
    void embeddedImagesUseDctDecodeWherePossible() throws Exception {
        byte[] pdf = new KotakPdfService().generate(sampleStatement(), false);
        String raw = new String(pdf, java.nio.charset.StandardCharsets.ISO_8859_1);
        assertTrue(raw.contains("/Filter/DCTDecode") || raw.contains("/Filter /DCTDecode"),
                "Expected at least one DCTDecode (JPEG) image XObject");
        assertFalse(raw.contains("iText"), "Kotak PDF must not contain iText producer branding");
    }

    private static String extractInfoDictionarySnippet(byte[] pdf) {
        String raw = new String(pdf, java.nio.charset.StandardCharsets.ISO_8859_1);
        int infoPos = raw.indexOf("/Producer");
        if (infoPos < 0) {
            infoPos = raw.indexOf("/CreationDate");
        }
        assertTrue(infoPos >= 0, "Info dictionary not found in PDF bytes");
        int start = Math.max(0, infoPos - 80);
        int end = Math.min(raw.length(), infoPos + 120);
        return raw.substring(start, end);
    }

    @Test
    void accountHolderNameMatchesOnAllPages() throws Exception {
        String rawName = "  AJAY\u00A0 SINGH SO SIDDNATH  ";
        String expected = "AJAY SINGH SO SIDDNATH";
        Statement statement = multiPageStatement(rawName);

        byte[] pdf = new KotakPdfService().generate(statement, false);
        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf));
             PdfDocument doc = new PdfDocument(reader)) {
            assertTrue(doc.getNumberOfPages() >= 2, "Expected multi-page PDF for header audit");
            for (int page = 1; page <= doc.getNumberOfPages(); page++) {
                final int pageNum = page;
                String text = PdfTextExtractor.getTextFromPage(doc.getPage(page));
                if (pageNum == 1 || text.contains("Account No.")) {
                    assertTrue(text.contains(expected),
                            () -> "Page " + pageNum + " missing normalized holder name. Text:\n" + text);
                }
            }
        }
    }

    private Statement multiPageStatement(String customerName) {
        Statement statement = sampleStatement();
        statement.getCustomerDetails().setCustomerName(customerName);
        java.util.ArrayList<Transaction> txns = new java.util.ArrayList<>();
        for (int i = 0; i < 90; i++) {
            txns.add(Transaction.builder()
                    .date(LocalDate.of(2026, 5, 1).plusDays(i % 23))
                    .narration("UPI/party" + i + "/6034546318" + String.format("%02d", i % 100) + "/Paid securely")
                    .reference("UPI-6034038455" + String.format("%02d", i % 100))
                    .debit(new BigDecimal("16500.00"))
                    .balance(new BigDecimal("95000.00"))
                    .build());
        }
        statement.setTransactions(txns);
        statement.setOpeningBalance(new BigDecimal("250000.00"));
        return statement;
    }

    private Statement sampleStatement() {
        return Statement.builder()
                .bankCode("KOTAK")
                .customerDetails(CustomerDetails.builder()
                        .customerName("Test User")
                        .address("Line 1")
                        .city("Bhopal")
                        .state("Madhya Pradesh")
                        .pincode("462001")
                        .build())
                .accountDetails(AccountDetails.builder()
                        .accountNumber("1234567890")
                        .accountType("Savings")
                        .branchName("Test Branch")
                        .ifscCode("KKBK0001234")
                        .build())
                .period(StatementPeriod.builder()
                        .fromDate(LocalDate.of(2026, 5, 1))
                        .toDate(LocalDate.of(2026, 5, 23))
                        .build())
                .openingBalance(new BigDecimal("10000.00"))
                .closingBalance(new BigDecimal("9500.00"))
                .transactions(List.of(
                        Transaction.builder()
                                .date(LocalDate.of(2026, 5, 10))
                                .narration("UPI/harshmeena/603454631871/Paid securely")
                                .reference("UPI-603403845545")
                                .debit(new BigDecimal("150.00"))
                                .balance(new BigDecimal("9500.00"))
                                .build()
                ))
                .build();
    }
}
