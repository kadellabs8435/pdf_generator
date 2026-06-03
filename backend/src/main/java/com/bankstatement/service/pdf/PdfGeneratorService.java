package com.bankstatement.service.pdf;

import com.bankstatement.config.AppProperties;
import com.bankstatement.entity.BankTemplate;
import com.bankstatement.entity.Statement;
import com.bankstatement.entity.Transaction;
import com.bankstatement.exception.ApiException;
import com.bankstatement.service.template.TemplateService;
import com.bankstatement.service.transaction.TransactionAmountGuard;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PdfGeneratorService {

    private static final DateTimeFormatter SBI_DATE_DASH = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter KOTAK_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter GENERATED_ON = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);

    private final List<BankPdfService> bankPdfServices;
    private final TemplateEngine templateEngine;
    private final TemplateService templateService;
    private final AppProperties appProperties;

    public byte[] renderPreview(Statement statement) {
        return renderPdf(statement, true);
    }

    public byte[] renderAndStore(Statement statement) {
        byte[] pdf = renderPdf(statement, false, true);
        try {
            Path storageDir = Paths.get(appProperties.getPdf().getStoragePath());
            Files.createDirectories(storageDir);
            String fileName = statement.getId() + "_" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
            Path filePath = storageDir.resolve(fileName);
            Files.write(filePath, pdf);
            statement.setPdfPath(filePath.toString());
        } catch (IOException e) {
            throw new ApiException("Failed to store PDF: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return pdf;
    }

    public byte[] loadStoredPdf(Statement statement) {
        if (statement.getPdfPath() == null) {
            throw new ApiException("PDF not generated yet", HttpStatus.NOT_FOUND.value());
        }
        try {
            return Files.readAllBytes(Paths.get(statement.getPdfPath()));
        } catch (IOException e) {
            throw new ApiException("Failed to read PDF", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    public String resolveDownloadFilename(Statement statement) {
        BankPdfService service = resolveBankService(statement.getBankCode());
        if (service != null) {
            return service.buildDownloadFilename(statement);
        }
        String account = statement.getAccountDetails() != null
                ? statement.getAccountDetails().getAccountNumber() : "statement";
        return "statement_" + account + ".pdf";
    }

    private byte[] renderPdf(Statement statement, boolean includeWatermark) {
        return renderPdf(statement, includeWatermark, false);
    }

    private byte[] renderPdf(Statement statement, boolean includeWatermark, boolean forDownload) {
        TransactionAmountGuard.prepareForRender(
                statement.getTransactions(), statement.getOpeningBalance());
        BankPdfService bankService = resolveBankService(statement.getBankCode());
        if (bankService != null) {
            if (forDownload) {
                return bankService.generateForDownload(statement);
            }
            return bankService.generate(statement, includeWatermark);
        }
        return renderHtmlPdf(statement, includeWatermark);
    }

    private BankPdfService resolveBankService(String bankCode) {
        if (bankCode == null) return null;
        for (BankPdfService service : bankPdfServices) {
            if (service.supports(bankCode)) {
                return service;
            }
        }
        return null;
    }

    private byte[] renderHtmlPdf(Statement statement, boolean includeWatermark) {
        BankTemplate template = templateService.getEntityByCode(statement.getBankCode());
        String templateName = template.getHtmlTemplatePath()
                .replace("templates/banks/", "banks/")
                .replace(".html", "");

        var account = statement.getAccountDetails();
        Map<String, BigDecimal> totals = computeTotals(statement);

        Context context = new Context();
        context.setVariable("statement", statement);
        context.setVariable("customer", statement.getCustomerDetails());
        context.setVariable("account", account);
        context.setVariable("period", statement.getPeriod());
        context.setVariable("transactions", statement.getTransactions());
        context.setVariable("openingBalance", formatMoney(statement.getOpeningBalance()));
        context.setVariable("closingBalance", formatMoney(
                statement.getClosingBalance() != null ? statement.getClosingBalance() : statement.getOpeningBalance()));
        context.setVariable("bankName", template.getDisplayName());
        context.setVariable("bankCode", template.getCode());
        context.setVariable("watermark", includeWatermark ? "SAMPLE / FOR DEMO ONLY" : "");
        context.setVariable("statementGeneratedOn", LocalDateTime.now().format(GENERATED_ON));
        context.setVariable("statementDate", LocalDate.now().format(SBI_DATE_DASH));
        context.setVariable("periodFromSbi", formatDateSbiDash(statement.getPeriod().getFromDate()));
        context.setVariable("periodToSbi", formatDateSbiDash(statement.getPeriod().getToDate()));
        context.setVariable("periodFromKotak", formatDateKotak(statement.getPeriod().getFromDate()));
        context.setVariable("periodToKotak", formatDateKotak(statement.getPeriod().getToDate()));
        context.setVariable("openingBalanceIndian", formatIndianMoney(statement.getOpeningBalance()));
        context.setVariable("closingBalanceIndian", formatIndianMoney(
                statement.getClosingBalance() != null ? statement.getClosingBalance() : statement.getOpeningBalance()));
        context.setVariable("totalDebit", formatMoney(totals.get("debit")));
        context.setVariable("totalCredit", formatMoney(totals.get("credit")));
        context.setVariable("totalDebitIndian", formatIndianMoney(totals.get("debit")));
        context.setVariable("totalCreditIndian", formatIndianMoney(totals.get("credit")));
        context.setVariable("debitCount", countDebits(statement));
        context.setVariable("creditCount", countCredits(statement));
        context.setVariable("micrCode", deriveMicr(account.getIfscCode()));
        context.setVariable("branchCode", deriveBranchCode(account.getIfscCode()));

        String html = templateEngine.process(templateName, context);
        html = html.replace("&nbsp;", "&#160;");

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, new ClassPathResource("templates/banks/").getURL().toString());
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new ApiException("PDF generation failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    private Map<String, BigDecimal> computeTotals(Statement statement) {
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
        if (statement.getTransactions() != null) {
            for (Transaction txn : statement.getTransactions()) {
                if (txn.getDebit() != null) debit = debit.add(txn.getDebit());
                if (txn.getCredit() != null) credit = credit.add(txn.getCredit());
            }
        }
        Map<String, BigDecimal> totals = new HashMap<>();
        totals.put("debit", debit);
        totals.put("credit", credit);
        return totals;
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%,.2f", amount);
    }

    private String formatIndianMoney(BigDecimal amount) {
        if (amount == null) return "0.00";
        String plain = amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        String[] parts = plain.split("\\.");
        String intPart = parts[0];
        String decPart = parts.length > 1 ? parts[1] : "00";
        if (intPart.length() <= 3) {
            return intPart + "." + decPart;
        }
        String last3 = intPart.substring(intPart.length() - 3);
        String rest = intPart.substring(0, intPart.length() - 3);
        StringBuilder grouped = new StringBuilder();
        while (rest.length() > 2) {
            grouped.insert(0, "," + rest.substring(rest.length() - 2));
            rest = rest.substring(0, rest.length() - 2);
        }
        if (!rest.isEmpty()) {
            grouped.insert(0, rest);
        }
        return grouped + "," + last3 + "." + decPart;
    }

    private String formatDateSbiDash(LocalDate date) {
        return date == null ? "" : date.format(SBI_DATE_DASH);
    }

    private String formatDateKotak(LocalDate date) {
        return date == null ? "" : date.format(KOTAK_DATE);
    }

    private int countDebits(Statement statement) {
        if (statement.getTransactions() == null) return 0;
        return (int) statement.getTransactions().stream()
                .filter(t -> t.getDebit() != null && t.getDebit().compareTo(BigDecimal.ZERO) > 0)
                .count();
    }

    private int countCredits(Statement statement) {
        if (statement.getTransactions() == null) return 0;
        return (int) statement.getTransactions().stream()
                .filter(t -> t.getCredit() != null && t.getCredit().compareTo(BigDecimal.ZERO) > 0)
                .count();
    }

    private String deriveMicr(String ifsc) {
        if (ifsc == null || ifsc.length() < 6) return "000000000";
        return ifsc.substring(ifsc.length() - 6) + "042";
    }

    private String deriveBranchCode(String ifsc) {
        if (ifsc == null || ifsc.length() < 4) return "0000";
        return ifsc.substring(ifsc.length() - 4);
    }
}
