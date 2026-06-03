package com.bankstatement.service.pdf;

import com.bankstatement.entity.Statement;

/** PDF document metadata matching real bank statement exports. */
record BankPdfDocumentInfo(
        String title,
        String author,
        String creator,
        String subject,
        String producer) {

    static BankPdfDocumentInfo forSbi(Statement statement) {
        return forBank(statement, "State Bank of India");
    }

    static BankPdfDocumentInfo forKotak(Statement statement) {
        return forBank(statement, "Kotak Mahindra Bank Ltd.");
    }

    static BankPdfDocumentInfo forBoi(Statement statement) {
        return new BankPdfDocumentInfo(
                "",
                "",
                "",
                "",
                BoiItext5Finalizer.PRODUCER);
    }

    private static BankPdfDocumentInfo forBank(Statement statement, String bankName) {
        String accountType = "Savings";
        if (statement.getAccountDetails() != null && statement.getAccountDetails().getAccountType() != null) {
            accountType = statement.getAccountDetails().getAccountType();
        }
        return new BankPdfDocumentInfo(
                "Account Statement",
                bankName,
                bankName,
                accountType + " Account Statement",
                "bank statement generator");
    }
}
