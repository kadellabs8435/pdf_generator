package com.bankstatement.service.pdf;

import com.bankstatement.entity.AccountDetails;
import com.bankstatement.entity.CustomerDetails;
import com.bankstatement.entity.Statement;
import com.bankstatement.entity.StatementPeriod;
import com.bankstatement.entity.Transaction;
import com.bankstatement.exception.ApiException;
import com.bankstatement.service.transaction.TransactionAmountGuard;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class BoiPdfService implements BankPdfService {

    private static final String LOGO_PATH = "pdf/boi/boi-logo.png";
    private static final DateTimeFormatter DATE_SLASH = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_DASH = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Override
    public boolean supports(String bankCode) {
        return "BOI".equalsIgnoreCase(bankCode);
    }

    @Override
    public byte[] generate(Statement statement, boolean includeWatermark) {
        return buildPdf(statement);
    }

    @Override
    public byte[] generateForDownload(Statement statement) {
        try {
            byte[] pdfBytes = buildPdf(statement);
            String password = BoiPdfPassword.generate(statement.getCustomerDetails());
            return BoiPdfEncryptor.encrypt(pdfBytes, password);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("BOI PDF download encryption failed", e);
            throw new ApiException("BOI PDF encryption failed: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    private byte[] buildPdf(Statement statement) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            BoiPdfStyles.clearCache();
            BankPdfImageCache imageCache = new BankPdfImageCache();

            CustomerDetails customer = statement.getCustomerDetails();
            String rawHolderName = customer != null ? customer.getCustomerName() : null;
            String accountHolderName = BoiAccountHolderNameFormatter.forPdf(rawHolderName);
            log.info(
                    "BOI PDF generated bankCode=BOI accountHolderNameRaw=\"{}\" accountHolderNameRendered=\"{}\"",
                    nullToEmpty(rawHolderName),
                    accountHolderName);

            PdfWriter writer = BoiPdfDocumentFactory.createWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            BoiPdfDocumentFactory.applyDocumentInfo(pdf, BankPdfDocumentInfo.forBoi(statement));

            Document doc = new Document(pdf, PageSize.A4);
            doc.setMargins(BoiPdfStyles.MARGIN_TOP, BoiPdfStyles.MARGIN_RIGHT,
                    BoiPdfStyles.MARGIN_BOTTOM, BoiPdfStyles.MARGIN_LEFT);

            addLogoRow(doc, imageCache);
            addTitle(doc);
            addStatementDate(doc, statement);
            addCustomerBox(doc, statement, accountHolderName);
            addFilterSection(doc, statement);
            addTransactionTypeLine(doc);
            doc.add(buildTransactionTable(statement));
            addFooterNote(doc);

            doc.close();
            byte[] pdfBytes = BoiPdfDocumentFactory.finalizeStructure(
                    baos.toByteArray(), BankPdfDocumentInfo.forBoi(statement));
            return BoiItext5Finalizer.finalizePreview(pdfBytes);
        } catch (Exception e) {
            log.error("BOI PDF generation failed", e);
            throw new ApiException("BOI PDF generation failed: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @Override
    public String buildDownloadFilename(Statement statement) {
        String account = statement.getAccountDetails() != null
                ? statement.getAccountDetails().getAccountNumber() : "unknown";
        return "BOI_Statement_" + account + ".pdf";
    }

    private void addLogoRow(Document doc, BankPdfImageCache imageCache) {
        Table row = new Table(UnitValue.createPercentArray(new float[]{55, 45}))
                .useAllAvailableWidth()
                .setMarginBottom(0);

        row.addCell(emptyCell());

        Image logo = imageCache.scaledImage(LOGO_PATH, BoiPdfStyles.LOGO_HEIGHT);
        logo.setHorizontalAlignment(HorizontalAlignment.RIGHT);
        logo.setMarginRight(BoiPdfStyles.LOGO_SHIFT_RIGHT);
        logo.setMarginTop(0);
        logo.setMarginBottom(0);
        logo.setMarginLeft(0);
        row.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .add(logo)
                .setTextAlignment(TextAlignment.RIGHT)
                .setHorizontalAlignment(HorizontalAlignment.RIGHT)
                .setVerticalAlignment(VerticalAlignment.TOP)
                .setPadding(0));

        doc.add(row);
    }

    private void addTitle(Document doc) {
        doc.add(new Paragraph("Detailed Statement")
                .setFont(BoiPdfStyles.bold())
                .setFontSize(BoiPdfStyles.FONT_TITLE)
                .setFontColor(BoiPdfStyles.BLACK)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(BoiPdfStyles.TITLE_MARGIN_TOP)
                .setMarginBottom(BoiPdfStyles.TITLE_MARGIN_BOTTOM)
                .setPaddingBottom(BoiPdfStyles.TITLE_PADDING_BOTTOM)
                .setMultipliedLeading(1.2f));
    }

    private void addStatementDate(Document doc, Statement statement) {
        LocalDate date = LocalDate.now();
        if (statement.getPeriod() != null && statement.getPeriod().getToDate() != null) {
            date = statement.getPeriod().getToDate();
        }
        doc.add(new Paragraph("Date: " + date.format(DATE_SLASH))
                .setFont(BoiPdfStyles.bold())
                .setFontSize(BoiPdfStyles.FONT_DATE)
                .setFontColor(BoiPdfStyles.BLACK)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(0)
                .setMarginBottom(BoiPdfStyles.DATE_MARGIN_BOTTOM));
    }

    private void addCustomerBox(Document doc, Statement statement, String accountHolderName) {
        CustomerDetails c = statement.getCustomerDetails();
        AccountDetails a = statement.getAccountDetails();

        String customerId = c != null ? nullToEmpty(c.getCustomerId()) : "";
        if (customerId.isBlank()) {
            customerId = deriveCustomerId(a != null ? a.getAccountNumber() : null);
        }
        String account = a != null ? nullToEmpty(a.getAccountNumber()) : "";
        String address = formatAddress(c);
        String ifsc = a != null ? nullToEmpty(a.getIfscCode()) : "";
        String branch = a != null ? nullToEmpty(a.getBranchName()) : "";

        Table outer = new Table(1).useAllAvailableWidth()
                .setMarginBottom(BoiPdfStyles.BOX_MARGIN_BOTTOM);
        Cell outerCell = new Cell()
                .setBorder(new SolidBorder(BoiPdfStyles.BORDER, BoiPdfStyles.DETAIL_BORDER_WIDTH))
                .setPaddingTop(BoiPdfStyles.DETAIL_PAD_V)
                .setPaddingBottom(BoiPdfStyles.DETAIL_PAD_V)
                .setPaddingLeft(BoiPdfStyles.DETAIL_PAD_H)
                .setPaddingRight(BoiPdfStyles.DETAIL_PAD_H);

        Table grid = new Table(UnitValue.createPointArray(BoiPdfStyles.CUSTOMER_GRID_COL_WIDTHS))
                .useAllAvailableWidth()
                .setFixedLayout();

        addCustomerGridRow(grid, "Account holder name", accountHolderName, "Account holder address", address, true);
        addCustomerGridRow(grid, "Customer ID", customerId, "IFSC", ifsc, true);
        addCustomerGridRow(grid, "Account number", account, "Branch Name", branch, false);

        outerCell.add(grid);
        outer.addCell(outerCell);
        doc.add(outer);
    }

    private void addCustomerGridRow(
            Table grid,
            String leftLabel,
            String leftValue,
            String rightLabel,
            String rightValue,
            boolean gapBelow) {
        float padBottom = gapBelow ? BoiPdfStyles.DETAIL_LINE_GAP : 0f;
        boolean holderNameRow = "Account holder name".equals(leftLabel);
        grid.addCell(detailLabelCell(leftLabel, padBottom));
        grid.addCell(detailValueCell(leftValue, padBottom, holderNameRow));
        grid.addCell(detailLabelCell(rightLabel, padBottom));
        grid.addCell(detailValueCell(rightValue, padBottom, false));
    }

    private void addFilterSection(Document doc, Statement statement) {
        StatementPeriod period = statement.getPeriod();
        String fromDate = period != null && period.getFromDate() != null
                ? period.getFromDate().format(DATE_DASH) : "-";
        String toDate = period != null && period.getToDate() != null
                ? period.getToDate().format(DATE_DASH) : "-";

        addFilterRow(doc, "Transaction Date", fromDate, toDate, true);
        addFilterRow(doc, "Amount", "-", "-", false);
        addFilterRow(doc, "Cheque", "-", "-", false);
    }

    private void addFilterRow(Document doc, String label, String fromVal, String toVal, boolean firstRow) {
        Table row = new Table(UnitValue.createPointArray(BoiPdfStyles.FILTER_COL_WIDTHS))
                .useAllAvailableWidth()
                .setMarginTop(firstRow ? BoiPdfStyles.FILTER_SECTION_MARGIN_TOP : 0)
                .setMarginBottom(BoiPdfStyles.FILTER_ROW_GAP);

        row.addCell(filterLabelCell(label));
        row.addCell(filterFromToCell("from: ", fromVal));
        row.addCell(filterFromToCell("to: ", toVal));

        doc.add(row);
    }

    private void addTransactionTypeLine(Document doc) {
        doc.add(new Paragraph("Transaction type: All")
                .setFont(BoiPdfStyles.bold())
                .setFontSize(BoiPdfStyles.FONT_FILTER)
                .setFontColor(BoiPdfStyles.BLACK)
                .setMarginTop(BoiPdfStyles.TXN_TYPE_MARGIN_TOP)
                .setMarginBottom(BoiPdfStyles.TXN_TYPE_MARGIN_BOTTOM));
    }

    private Table buildTransactionTable(Statement statement) {
        Table table = new Table(UnitValue.createPointArray(BoiPdfStyles.TXN_COL_WIDTHS))
                .useAllAvailableWidth()
                .setMarginTop(BoiPdfStyles.TABLE_MARGIN_TOP);

        addTxnSrNoHeader(table);
        addTxnHeader(table, "Date", TextAlignment.LEFT);
        addTxnHeader(table, "Remarks", TextAlignment.LEFT);
        addTxnHeader(table, "Debit", TextAlignment.LEFT);
        addTxnHeader(table, "Credit", TextAlignment.LEFT);
        addTxnHeader(table, "Balance", TextAlignment.LEFT);

        List<Transaction> txns = statement.getTransactions();
        if (txns != null) {
            int serial = 1;
            for (Transaction txn : txns) {
                addTxnRow(table, serial++, txn);
            }
        }

        return table;
    }

    private void addFooterNote(Document doc) {
        Div note = new Div()
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(BoiPdfStyles.NOTE_MARGIN_TOP)
                .setMarginLeft(0)
                .setMarginRight(0)
                .setPadding(0)
                .setKeepTogether(true);

        note.add(new Paragraph(BoiPdfStyles.NOTE_HEADING)
                .setFont(BoiPdfStyles.bold())
                .setFontSize(BoiPdfStyles.FONT_NOTE_HEADING)
                .setFontColor(BoiPdfStyles.BLACK)
                .setMargin(0)
                .setMarginBottom(BoiPdfStyles.NOTE_HEADING_MARGIN_BOTTOM)
                .setPadding(0)
                .setTextAlignment(TextAlignment.LEFT)
                .setMultipliedLeading(BoiPdfStyles.LINE_HEIGHT_NOTE));

        note.add(new Paragraph(BoiPdfStyles.NOTE_BODY)
                .setFont(BoiPdfStyles.regular())
                .setFontSize(BoiPdfStyles.FONT_NOTE_BODY)
                .setFontColor(BoiPdfStyles.BLACK)
                .setMargin(0)
                .setPadding(0)
                .setTextAlignment(TextAlignment.LEFT)
                .setMultipliedLeading(BoiPdfStyles.LINE_HEIGHT_NOTE));

        doc.add(note);
    }

    private void addTxnRow(Table table, int serial, Transaction txn) {
        table.addCell(txnBodyCell(String.valueOf(serial), TextAlignment.LEFT, false, false));
        table.addCell(txnDateCell(formatTxnDate(txn.getDate())));
        table.addCell(txnRemarksCell(nullToEmpty(txn.getNarration())));
        table.addCell(txnBodyCell(formatDr(TransactionAmountGuard.displayDebit(txn)), TextAlignment.RIGHT, true, false));
        table.addCell(txnBodyCell(formatCr(TransactionAmountGuard.displayCredit(txn)), TextAlignment.RIGHT, true, false));
        table.addCell(txnBodyCell(formatBalance(txn.getBalance()), TextAlignment.RIGHT, true, true));
    }

    private Cell detailValueCell(String value, float paddingBottom) {
        return detailValueCell(value, paddingBottom, false);
    }

    private Cell detailValueCell(String value, float paddingBottom, boolean keepTogether) {
        Paragraph paragraph = new Paragraph(value)
                .setFont(BoiPdfStyles.regular())
                .setFontSize(BoiPdfStyles.FONT_BODY)
                .setFontColor(BoiPdfStyles.TEXT_DARK)
                .setMargin(0)
                .setMultipliedLeading(BoiPdfStyles.LINE_HEIGHT_DETAIL);
        if (keepTogether) {
            paragraph.setKeepTogether(true);
        }
        return new Cell()
                .setBorder(Border.NO_BORDER)
                .add(paragraph)
                .setPadding(0)
                .setPaddingBottom(paddingBottom)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell detailLabelCell(String label, float paddingBottom) {
        return new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph(label + ":")
                        .setFont(BoiPdfStyles.regular())
                        .setFontSize(BoiPdfStyles.FONT_LABEL)
                        .setFontColor(BoiPdfStyles.TEXT_DARK)
                        .setMargin(0)
                        .setMultipliedLeading(BoiPdfStyles.LINE_HEIGHT_DETAIL))
                .setPadding(0)
                .setPaddingBottom(paddingBottom)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell filterLabelCell(String label) {
        return new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph(label)
                        .setFont(BoiPdfStyles.bold())
                        .setFontSize(BoiPdfStyles.FONT_FILTER)
                        .setFontColor(BoiPdfStyles.BLACK)
                        .setMargin(0)
                        .setMultipliedLeading(BoiPdfStyles.LINE_HEIGHT))
                .setPadding(0)
                .setVerticalAlignment(VerticalAlignment.TOP)
                .setKeepTogether(true);
    }

    private Cell filterFromToCell(String prefix, String value) {
        return new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph(prefix + value)
                        .setFont(BoiPdfStyles.bold())
                        .setFontSize(BoiPdfStyles.FONT_FILTER)
                        .setFontColor(BoiPdfStyles.BLACK)
                        .setMargin(0)
                        .setMultipliedLeading(BoiPdfStyles.LINE_HEIGHT))
                .setPadding(0)
                .setVerticalAlignment(VerticalAlignment.TOP)
                .setKeepTogether(true);
    }

    private void addTxnSrNoHeader(Table table) {
        table.addHeaderCell(new Cell()
                .add(new Paragraph("Sr No")
                        .setFont(BoiPdfStyles.regular())
                        .setFontSize(BoiPdfStyles.FONT_TABLE)
                        .setFontColor(BoiPdfStyles.BLACK)
                        .setMargin(0))
                .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.WHITE)
                .setBorder(new SolidBorder(BoiPdfStyles.BORDER, BoiPdfStyles.TXN_BORDER_WIDTH))
                .setPaddingTop(BoiPdfStyles.TXN_HEADER_PAD_V)
                .setPaddingBottom(BoiPdfStyles.TXN_HEADER_PAD_V)
                .setPaddingLeft(BoiPdfStyles.TXN_HEADER_PAD_H)
                .setPaddingRight(BoiPdfStyles.TXN_HEADER_PAD_H)
                .setMinHeight(BoiPdfStyles.TXN_HEADER_MIN_HEIGHT)
                .setTextAlignment(TextAlignment.LEFT)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setKeepTogether(true));
    }

    private void addTxnHeader(Table table, String title, TextAlignment align) {
        table.addHeaderCell(new Cell()
                .add(new Paragraph(title)
                        .setFont(BoiPdfStyles.regular())
                        .setFontSize(BoiPdfStyles.FONT_TABLE)
                        .setFontColor(BoiPdfStyles.BLACK)
                        .setMargin(0))
                .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.WHITE)
                .setBorder(new SolidBorder(BoiPdfStyles.BORDER, BoiPdfStyles.TXN_BORDER_WIDTH))
                .setPaddingTop(BoiPdfStyles.TXN_HEADER_PAD_V)
                .setPaddingBottom(BoiPdfStyles.TXN_HEADER_PAD_V)
                .setPaddingLeft(BoiPdfStyles.TXN_HEADER_PAD_H)
                .setPaddingRight(BoiPdfStyles.TXN_HEADER_PAD_H)
                .setMinHeight(BoiPdfStyles.TXN_HEADER_MIN_HEIGHT)
                .setTextAlignment(align)
                .setVerticalAlignment(VerticalAlignment.MIDDLE));
    }

    private Cell txnDateCell(String text) {
        return new Cell()
                .add(new Paragraph(text)
                        .setFont(BoiPdfStyles.regular())
                        .setFontSize(BoiPdfStyles.FONT_TABLE)
                        .setFontColor(BoiPdfStyles.BLACK)
                        .setMultipliedLeading(BoiPdfStyles.LINE_HEIGHT_TABLE)
                        .setMargin(0))
                .setBorder(new SolidBorder(BoiPdfStyles.BORDER, BoiPdfStyles.TXN_BORDER_WIDTH))
                .setPaddingTop(BoiPdfStyles.TXN_BODY_PAD_V)
                .setPaddingBottom(BoiPdfStyles.TXN_BODY_PAD_V)
                .setPaddingLeft(BoiPdfStyles.TXN_BODY_PAD_H)
                .setPaddingRight(BoiPdfStyles.TXN_BODY_PAD_H)
                .setMinHeight(BoiPdfStyles.TXN_BODY_MIN_HEIGHT)
                .setTextAlignment(TextAlignment.LEFT)
                .setVerticalAlignment(VerticalAlignment.TOP)
                .setKeepTogether(true);
    }

    private Cell txnBodyCell(String text, TextAlignment align, boolean amountColumn, boolean balanceColumn) {
        float padRight = BoiPdfStyles.TXN_BODY_PAD_H;
        if (balanceColumn) {
            padRight = BoiPdfStyles.TXN_BALANCE_PAD_RIGHT;
        } else if (amountColumn) {
            padRight = BoiPdfStyles.TXN_AMOUNT_PAD_RIGHT;
        }
        return new Cell()
                .add(new Paragraph(text)
                        .setFont(BoiPdfStyles.regular())
                        .setFontSize(BoiPdfStyles.FONT_TABLE)
                        .setFontColor(BoiPdfStyles.BLACK)
                        .setMultipliedLeading(BoiPdfStyles.LINE_HEIGHT_TABLE)
                        .setMargin(0))
                .setBorder(new SolidBorder(BoiPdfStyles.BORDER, BoiPdfStyles.TXN_BORDER_WIDTH))
                .setPaddingTop(BoiPdfStyles.TXN_BODY_PAD_V)
                .setPaddingBottom(BoiPdfStyles.TXN_BODY_PAD_V)
                .setPaddingLeft(BoiPdfStyles.TXN_BODY_PAD_H)
                .setPaddingRight(padRight)
                .setMinHeight(BoiPdfStyles.TXN_BODY_MIN_HEIGHT)
                .setTextAlignment(align)
                .setVerticalAlignment(VerticalAlignment.TOP)
                .setKeepTogether(true);
    }

    private Cell txnRemarksCell(String narration) {
        return new Cell()
                .add(new Paragraph(narration.toUpperCase(java.util.Locale.ROOT))
                        .setFont(BoiPdfStyles.regular())
                        .setFontSize(BoiPdfStyles.FONT_TABLE)
                        .setFontColor(BoiPdfStyles.BLACK)
                        .setMultipliedLeading(BoiPdfStyles.LINE_HEIGHT_TABLE)
                        .setMargin(0))
                .setBorder(new SolidBorder(BoiPdfStyles.BORDER, BoiPdfStyles.TXN_BORDER_WIDTH))
                .setPaddingTop(BoiPdfStyles.TXN_BODY_PAD_V)
                .setPaddingBottom(BoiPdfStyles.TXN_BODY_PAD_V)
                .setPaddingLeft(BoiPdfStyles.TXN_BODY_PAD_H)
                .setPaddingRight(BoiPdfStyles.TXN_BODY_PAD_H)
                .setMinHeight(BoiPdfStyles.TXN_BODY_MIN_HEIGHT)
                .setTextAlignment(TextAlignment.LEFT)
                .setVerticalAlignment(VerticalAlignment.TOP)
                .setKeepTogether(true);
    }

    private Cell emptyCell() {
        return new Cell().setBorder(Border.NO_BORDER).setPadding(0);
    }

    private String formatAddress(CustomerDetails c) {
        if (c == null) return "";
        StringBuilder sb = new StringBuilder();
        appendPart(sb, c.getAddress());
        appendPart(sb, c.getCity());
        appendPart(sb, c.getState());
        appendPart(sb, c.getPincode());
        return sb.toString().trim();
    }

    private void appendPart(StringBuilder sb, String part) {
        if (part == null || part.isBlank()) return;
        if (!sb.isEmpty()) sb.append(' ');
        sb.append(part.trim());
    }

    private String deriveCustomerId(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) return "";
        long hash = Math.abs(accountNumber.hashCode());
        return String.valueOf(100_000_000L + (hash % 900_000_000L));
    }

    private String formatTxnDate(LocalDate date) {
        return date == null ? "" : date.format(DATE_DASH);
    }

    private String formatDr(BigDecimal debit) {
        if (debit == null || debit.compareTo(BigDecimal.ZERO) == 0) return "";
        return formatPlainAmount(debit.abs());
    }

    private String formatCr(BigDecimal credit) {
        if (credit == null || credit.compareTo(BigDecimal.ZERO) == 0) return "";
        return formatPlainAmount(credit.abs());
    }

    private String formatPlainAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatBalance(BigDecimal balance) {
        if (balance == null) return "";
        return "\u20B9 " + formatIndianMoney(balance);
    }

    private String formatIndianMoney(BigDecimal amount) {
        String plain = amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
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

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
