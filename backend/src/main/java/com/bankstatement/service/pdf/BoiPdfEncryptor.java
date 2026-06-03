package com.bankstatement.service.pdf;

/** Applies Standard 128-bit RC4 encryption to finalized BOI PDF bytes. */
final class BoiPdfEncryptor {

    private BoiPdfEncryptor() {}

    static byte[] encrypt(byte[] pdfBytes, String password) throws Exception {
        return BoiItext5InfoPatch.encrypt(pdfBytes, password);
    }
}
