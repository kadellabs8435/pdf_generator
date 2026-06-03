package com.bankstatement.service.pdf;

import com.bankstatement.entity.Statement;

public interface BankPdfService {

    boolean supports(String bankCode);

    byte[] generate(Statement statement, boolean includeWatermark);

    /** Final PDF for download/storage; BOI applies password protection here only. */
    default byte[] generateForDownload(Statement statement) {
        return generate(statement, false);
    }

    String buildDownloadFilename(Statement statement);
}
