package com.bankstatement.service.pdf;

import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfObject;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.WriterProperties;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Post-processes SBI statement PDFs to align catalog structure and document metadata
 * with OpenPDF-style exports (Producer, catalog key order, optional XMP).
 */
public final class SbiOpenPdfMetadata {

    /** OpenPDF-style producer string (align with reference bank exports). */
    public static final String PRODUCER = "OpenPDF 2.0.3";

    /**
     * Reference creation date format used when aligning to a known bank export (Kotak reference).
     * Generated PDFs use current generation time for CreationDate/ModDate unless overridden.
     */
    public static final String REFERENCE_CREATION_DATE = "D:20260521163150Z";

    private SbiOpenPdfMetadata() {}

    /**
     * Applies metadata and catalog ordering fixes to generated PDF bytes.
     */
    public static byte[] apply(byte[] pdfBytes) throws IOException {
        return apply(pdfBytes, null);
    }

    public static byte[] apply(byte[] pdfBytes, Instant creationInstant) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfBytes));
            reader.setUnethicalReading(true);
            PdfWriter writer = new PdfWriter(out, writerProperties());
            PdfDocument pdf = new PdfDocument(reader, writer);

            reorderCatalogDictionary(pdf);
            patchDocumentInfo(pdf, creationInstant);

            pdf.close();
            return out.toByteArray();
        }
    }

    private static void patchDocumentInfo(PdfDocument pdf, Instant creationInstant) {
        pdf.getDocumentInfo().setProducer(PRODUCER);
        pdf.getDocumentInfo().setCreator("State Bank of India");
        pdf.getDocumentInfo().setAuthor("State Bank of India");
        pdf.getDocumentInfo().setTitle("Account Statement");
        pdf.getDocumentInfo().setSubject("Savings Account Statement");

        Instant instant = creationInstant != null ? creationInstant : Instant.now();
        String pdfDate = "D:" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .withZone(ZoneId.of("Asia/Kolkata"))
                .format(instant) + "+05'30'";
        pdf.getDocumentInfo().setMoreInfo(PdfName.CreationDate.getValue(), pdfDate);
        pdf.getDocumentInfo().setMoreInfo(PdfName.ModDate.getValue(), pdfDate);
    }

    /**
     * Ensures /Type appears before /Pages in the catalog dictionary (PDFBox convention).
     */
    private static void reorderCatalogDictionary(PdfDocument pdf) {
        PdfDictionary catalog = pdf.getCatalog().getPdfObject();
        if (catalog == null) {
            return;
        }
        PdfObject type = catalog.get(PdfName.Type);
        PdfObject pages = catalog.get(PdfName.Pages);
        catalog.remove(PdfName.Type);
        catalog.remove(PdfName.Pages);
        if (type != null) {
            catalog.put(PdfName.Type, type);
        }
        if (pages != null) {
            catalog.put(PdfName.Pages, pages);
        }
        pdf.getCatalog().setModified();
    }

    private static WriterProperties writerProperties() {
        return new WriterProperties()
                .setPdfVersion(com.itextpdf.kernel.pdf.PdfVersion.PDF_1_5)
                .setCompressionLevel(com.itextpdf.kernel.pdf.CompressionConstants.BEST_COMPRESSION)
                .useSmartMode();
    }
}