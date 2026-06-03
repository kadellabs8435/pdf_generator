package com.bankstatement.service.pdf;

import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.CompressionConstants;
import com.itextpdf.kernel.pdf.PdfArray;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfObject;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfStream;
import com.itextpdf.kernel.pdf.PdfVersion;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.WriterProperties;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;

/** Creates bank PDFs with version, compression, metadata, and structural normalization. */
final class BankPdfDocumentFactory {

    private BankPdfDocumentFactory() {}

    static WriterProperties writerProperties() {
        return new WriterProperties()
                .setPdfVersion(PdfVersion.PDF_1_5)
                .setCompressionLevel(CompressionConstants.BEST_COMPRESSION)
                .useSmartMode();
    }

    static PdfWriter createWriter(OutputStream outputStream) {
        return new PdfWriter(outputStream, writerProperties());
    }

    static void applyDocumentInfo(PdfDocument pdf, BankPdfDocumentInfo info) {
        pdf.getDocumentInfo().setTitle(info.title());
        pdf.getDocumentInfo().setAuthor(info.author());
        pdf.getDocumentInfo().setCreator(info.creator());
        pdf.getDocumentInfo().setSubject(info.subject());
        pdf.getDocumentInfo().setProducer(info.producer());
        pdf.getDocumentInfo().addCreationDate();
        pdf.getDocumentInfo().addModDate();
    }

    /**
     * Normalizes page structure: single content stream, MediaBox only, transparency group, PDF 1.5.
     */
    static byte[] finalizeKotakStructure(byte[] pdfBytes,
                                         java.util.function.Consumer<PdfDocument> beforeClose) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfBytes));
            reader.setUnethicalReading(true);
            PdfWriter writer = createWriter(out);
            PdfDocument pdf = new PdfDocument(reader, writer);

            if (beforeClose != null) {
                beforeClose.accept(pdf);
            }

            for (int i = 1; i <= pdf.getNumberOfPages(); i++) {
                normalizePage(pdf.getPage(i), pdf);
            }

            pdf.close();
            return KotakOpenPdfMetadata.apply(out.toByteArray());
        }
    }

    static byte[] finalizeStructure(byte[] pdfBytes, BankPdfDocumentInfo info) throws IOException {
        return finalizeStructure(pdfBytes, info, null);
    }

    static byte[] finalizeStructure(byte[] pdfBytes, BankPdfDocumentInfo info,
                                    java.util.function.Consumer<PdfDocument> beforeClose) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfBytes));
            reader.setUnethicalReading(true);
            PdfWriter writer = createWriter(out);
            PdfDocument pdf = new PdfDocument(reader, writer);

            if (beforeClose != null) {
                beforeClose.accept(pdf);
            }

            for (int i = 1; i <= pdf.getNumberOfPages(); i++) {
                normalizePage(pdf.getPage(i), pdf);
            }

            if (!BankPdfDocumentInfo.isSbi(info)) {
                applyDocumentInfo(pdf, info);
            }
            pdf.close();
            byte[] result = out.toByteArray();
            if (BankPdfDocumentInfo.isSbi(info)) {
                return SbiOpenPdfMetadata.apply(result);
            }
            return result;
        }
    }

    private static void normalizePage(PdfPage page, PdfDocument pdf) {
        page.setMediaBox(new Rectangle(PageSize.A4));
        page.getPdfObject().remove(PdfName.TrimBox);
        page.getPdfObject().remove(PdfName.CropBox);
        page.getPdfObject().remove(PdfName.BleedBox);
        page.getPdfObject().remove(PdfName.ArtBox);
        addDeviceRgbTransparencyGroup(page);
        mergeContentStreams(page, pdf);
    }

    private static void addDeviceRgbTransparencyGroup(PdfPage page) {
        PdfDictionary group = new PdfDictionary();
        group.put(PdfName.S, PdfName.Transparency);
        group.put(PdfName.CS, PdfName.DeviceRGB);
        page.getPdfObject().put(PdfName.Group, group);
    }

    private static void mergeContentStreams(PdfPage page, PdfDocument pdf) {
        PdfObject contentsObj = page.getPdfObject().get(PdfName.Contents);
        if (contentsObj == null) {
            return;
        }

        if (contentsObj.isStream()) {
            return;
        }

        if (!contentsObj.isArray()) {
            return;
        }

        PdfArray contentsArray = (PdfArray) contentsObj;
        if (contentsArray.size() <= 1) {
            if (contentsArray.size() == 1) {
                page.getPdfObject().put(PdfName.Contents, contentsArray.get(0));
                page.setModified();
            }
            return;
        }

        ByteArrayOutputStream merged = new ByteArrayOutputStream();
        for (int i = 0; i < contentsArray.size(); i++) {
            PdfStream stream = contentsArray.getAsStream(i);
            if (stream != null) {
                merged.writeBytes(stream.getBytes());
                merged.write('\n');
            }
        }

        PdfStream mergedStream = new PdfStream(merged.toByteArray());
        mergedStream.setCompressionLevel(CompressionConstants.BEST_COMPRESSION);
        mergedStream.makeIndirect(pdf);
        page.getPdfObject().put(PdfName.Contents, mergedStream);
        page.setModified();
    }
}
