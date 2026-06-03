package com.bankstatement.service.pdf;

import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;

/**
 * BOI-only PDF finalization using iText 5.5.13.3 page copy + exact metadata patch.
 * Layout is rendered with iText 7; output metadata/encryption match real BOI exports.
 */
final class BoiItext5Finalizer {

    /** Exact Producer string required for BOI PDF metadata. */
    static final String PRODUCER = "iText\u00AE 5.5.13.3";

    private BoiItext5Finalizer() {}

    static byte[] finalizePreview(byte[] pdfBytes) throws Exception {
        PdfReader reader = new PdfReader(pdfBytes);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document();
            document.addTitle("");
            document.addAuthor("");
            document.addSubject("");
            document.addKeywords("");
            document.addCreator("");

            PdfCopy copy = new PdfCopy(document, out);
            copy.setPdfVersion(PdfWriter.VERSION_1_7);
            document.open();
            int pages = reader.getNumberOfPages();
            for (int i = 1; i <= pages; i++) {
                copy.addPage(copy.getImportedPage(reader, i));
            }
            document.close();
            return BoiItext5InfoPatch.apply(out.toByteArray());
        } finally {
            reader.close();
        }
    }
}
