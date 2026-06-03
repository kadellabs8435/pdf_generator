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
 * Rewrites Kotak PDF Info/Catalog to match the FinBox-accepted OpenPDF reference export.
 * Values are taken only from the original Kotak metadata dump (no invented producer strings).
 */
final class KotakOpenPdfMetadata {

    /** Original Kotak reference: {@code /Producer (OpenPDF 2.0.3)} */
    static final String OPENPDF_PRODUCER = "OpenPDF 2.0.3";

    /** Original Kotak reference: {@code /CreationDate (D:20260521163150Z)} */
    static final String REFERENCE_CREATION_DATE = "D:20260521163150Z";

    private KotakOpenPdfMetadata() {}

    static byte[] apply(byte[] pdfBytes) throws IOException {
        try (PDDocument doc = PDDocument.load(pdfBytes);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            applyInfoDictionary(doc);
            reorderCatalogDictionary(doc.getDocumentCatalog());
            doc.getDocumentCatalog().setMetadata(null);
            doc.save(out);
            return out.toByteArray();
        }
    }

    private static void applyInfoDictionary(PDDocument doc) {
        COSDictionary infoDict = new COSDictionary();
        infoDict.setItem(COSName.CREATION_DATE, new COSString(REFERENCE_CREATION_DATE));
        infoDict.setItem(COSName.PRODUCER, new COSString(OPENPDF_PRODUCER));
        doc.setDocumentInformation(new PDDocumentInformation(infoDict));
    }

    /** Original catalog: {@code <</Type/Catalog/Pages 7 0 R>>} — /Type before /Pages. */
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
