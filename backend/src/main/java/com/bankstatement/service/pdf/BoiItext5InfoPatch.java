package com.bankstatement.service.pdf;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** BOI-only Info dictionary patch and Standard 128-bit RC4 encryption. */
final class BoiItext5InfoPatch {

    private BoiItext5InfoPatch() {}

    static byte[] apply(byte[] pdfBytes) throws IOException {
        try (PDDocument doc = PDDocument.load(pdfBytes);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            patchInfo(doc);
            doc.save(out);
            return out.toByteArray();
        }
    }

    static byte[] encrypt(byte[] pdfBytes, String password) throws IOException {
        try (PDDocument doc = PDDocument.load(pdfBytes);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            patchInfo(doc);
            AccessPermission permissions = new AccessPermission();
            permissions.setCanPrint(true);
            StandardProtectionPolicy policy = new StandardProtectionPolicy(password, password, permissions);
            policy.setEncryptionKeyLength(128);
            policy.setPreferAES(false);
            doc.protect(policy);
            doc.save(out);
            return out.toByteArray();
        }
    }

    private static void patchInfo(PDDocument doc) {
        doc.setVersion(1.7f);
        COSDictionary infoDict = new COSDictionary();
        infoDict.setString(COSName.PRODUCER, BoiItext5Finalizer.PRODUCER);
        infoDict.setString(COSName.TITLE, "");
        infoDict.setString(COSName.AUTHOR, "");
        infoDict.setString(COSName.SUBJECT, "");
        infoDict.setString(COSName.KEYWORDS, "");
        doc.setDocumentInformation(new PDDocumentInformation(infoDict));
    }
}
