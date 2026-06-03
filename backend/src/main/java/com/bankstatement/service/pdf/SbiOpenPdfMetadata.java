package com.bankstatement.service.pdf;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rewrites SBI PDF Info/Catalog to match approved JasperReports/OpenPDF reference exports.
 */
public final class SbiOpenPdfMetadata {

    /** Approved SBI reference: {@code /Producer (OpenPDF 1.3.32)} */
    public static final String PRODUCER = "OpenPDF 1.3.32";

    /** Approved SBI reference JasperReports creator string. */
    public static final String CREATOR =
            "JasperReports Library version 6.21.2-8434a0bd7c3bbc37cbf916f2968d35e4b165821a";

    private SbiOpenPdfMetadata() {}

    public static byte[] apply(byte[] pdfBytes) throws IOException {
        try (PDDocument doc = PDDocument.load(pdfBytes);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            applyInfoDictionary(doc);
            reorderCatalogDictionary(doc.getDocumentCatalog());
            doc.getDocumentCatalog().setMetadata(null);
            doc.save(out);
            return out.toByteArray();
        }
    }

    /** Only Creator and Producer — no Title, Subject, or Author. */
    private static void applyInfoDictionary(PDDocument doc) {
        COSDictionary infoDict = new COSDictionary();
        infoDict.setItem(COSName.CREATOR, new COSString(CREATOR));
        infoDict.setItem(COSName.PRODUCER, new COSString(PRODUCER));
        doc.setDocumentInformation(new PDDocumentInformation(infoDict));
    }

    /** Ensures /Type appears before /Pages in the catalog dictionary. */
    private static void reorderCatalogDictionary(PDDocumentCatalog catalog) {
        COSDictionary cos = catalog.getCOSObject();
        Map<COSName, org.apache.pdfbox.cos.COSBase> entries = new LinkedHashMap<>();
        for (COSName key : cos.keySet()) {
            entries.put(key, cos.getDictionaryObject(key));
        }
        cos.clear();
        cos.setItem(COSName.TYPE, entries.getOrDefault(COSName.TYPE, COSName.CATALOG));
        cos.setItem(COSName.PAGES, entries.get(COSName.PAGES));
        for (Map.Entry<COSName, org.apache.pdfbox.cos.COSBase> entry : entries.entrySet()) {
            if (entry.getKey() != COSName.TYPE && entry.getKey() != COSName.PAGES) {
                cos.setItem(entry.getKey(), entry.getValue());
            }
        }
    }
}
