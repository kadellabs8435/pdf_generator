package com.bankstatement.service.pdf;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class SbiOpenPdfMetadataTest {

    @Test
    void applyMatchesApprovedInfoDictionary() throws Exception {
        byte[] draft;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = BankPdfDocumentFactory.createWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);
            doc.add(new Paragraph("test"));
            doc.close();
            draft = baos.toByteArray();
        }

        byte[] patched = SbiOpenPdfMetadata.apply(draft);

        try (PDDocument doc = PDDocument.load(patched)) {
            var info = doc.getDocumentInformation();
            assertEquals(SbiOpenPdfMetadata.CREATOR, info.getCreator());
            assertEquals(SbiOpenPdfMetadata.PRODUCER, info.getProducer());
            assertNull(info.getTitle());
            assertNull(info.getSubject());
            assertNull(info.getAuthor());
            assertFalse(doc.getDocumentCatalog().getMetadata() != null);
        }

        String raw = new String(patched);
        assertFalse(raw.contains("modified using iText"), raw);
        assertFalse(raw.contains("/Title"), raw);
        assertFalse(raw.contains("/Subject"), raw);
        assertFalse(raw.contains("/Author"), raw);
    }
}
