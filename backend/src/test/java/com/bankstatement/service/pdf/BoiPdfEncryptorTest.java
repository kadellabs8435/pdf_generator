package com.bankstatement.service.pdf;

import com.itextpdf.text.pdf.PdfReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoiPdfEncryptorTest {

    @Test
    void encryptsPdfRequiresCorrectPassword() throws Exception {
        byte[] sample = BoiItext5Finalizer.finalizePreview(
                new BoiPdfService().generate(sampleStatement(), false));
        String password = "PAVA2401";
        byte[] encrypted = BoiPdfEncryptor.encrypt(sample, password);

        assertTrue(encrypted.length > 100);
        assertNotEquals(sample.length, encrypted.length);
        assertTrue(new String(encrypted, 0, Math.min(encrypted.length, 16)).startsWith("%PDF-1.7"));

        assertThrows(Exception.class, () -> new PdfReader(encrypted));
        PdfReader reader = new PdfReader(encrypted, password.getBytes());
        try {
            assertTrue(reader.getNumberOfPages() >= 1);
            assertEquals(BoiItext5Finalizer.PRODUCER, reader.getInfo().get("Producer"));
        } finally {
            reader.close();
        }
    }

    private com.bankstatement.entity.Statement sampleStatement() {
        return com.bankstatement.entity.Statement.builder()
                .bankCode("BOI")
                .customerDetails(com.bankstatement.entity.CustomerDetails.builder()
                        .customerName("PAVAN SINGH")
                        .customerId("123456789")
                        .dateOfBirth(java.time.LocalDate.of(1998, 1, 24))
                        .address("TEST")
                        .city("CITY")
                        .pincode("123456")
                        .build())
                .accountDetails(com.bankstatement.entity.AccountDetails.builder()
                        .accountNumber("995610110012688")
                        .branchName("BRANCH")
                        .ifscCode("BKID0009956")
                        .build())
                .period(com.bankstatement.entity.StatementPeriod.builder()
                        .fromDate(java.time.LocalDate.of(2025, 12, 1))
                        .toDate(java.time.LocalDate.of(2026, 5, 24))
                        .build())
                .openingBalance(new java.math.BigDecimal("5000.00"))
                .transactions(java.util.List.of())
                .build();
    }
}
