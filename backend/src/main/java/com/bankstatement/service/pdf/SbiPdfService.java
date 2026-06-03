package com.bankstatement.service.pdf;

import com.bankstatement.entity.AccountDetails;
import com.bankstatement.entity.CustomerDetails;
import com.bankstatement.entity.Statement;
import com.bankstatement.entity.StatementPeriod;
import com.bankstatement.entity.Transaction;
import com.bankstatement.exception.ApiException;
import com.bankstatement.service.transaction.TransactionAmountGuard;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.BorderRadius;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class SbiPdfService implements BankPdfService {

    private static final DateTimeFormatter DATE_SLASH = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_DASH = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Override
    public boolean supports(String bankCode) {
        return "SBI".equalsIgnoreCase(bankCode);
    }

    @Override
    public byte[] generate(Statement statement, boolean includeWatermark) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            SbiPdfStyles.clearCache();
            SbiPdfIcons.clearCache();
            BankPdfImageCache imageCache = new BankPdfImageCache();

            PdfWriter writer = BankPdfDocumentFactory.createWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            BankPdfDocumentFactory.applyDocumentInfo(pdf, BankPdfDocumentInfo.forSbi(statement));

            SbiPageContext pageContext = new SbiPageContext();
            pdf.addEventHandler(PdfDocumentEvent.START_PAGE, new SbiBannerBackgroundHandler());
            pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new SbiPageEventHandler(pageContext, includeWatermark));

            Document doc = new Document(pdf, PageSize.A4);
            doc.setMargins(SbiPdfStyles.MARGIN_TOP, SbiPdfStyles.MARGIN_RIGHT,
                    SbiPdfStyles.MARGIN_BOTTOM, SbiPdfStyles.MARGIN_LEFT);

            addTopBanner(doc, statement, imageCache);
            addTitleSection(doc);
            addCustomerSection(doc, statement);
            addPeriodLine(doc, statement);
            doc.add(buildTransactionTable(statement));

            doc.flush();
            pageContext.setSummaryStartPage(pdf.getNumberOfPages() + 1);
            doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            doc.setMargins(SbiPdfStyles.MARGIN_TOP, SbiPdfStyles.MARGIN_RIGHT,
                    SbiPdfStyles.SUMMARY_MARGIN_BOTTOM, SbiPdfStyles.MARGIN_LEFT);

            addSummarySection(doc, statement);
            addInstructions(doc);

            doc.close();
            return BankPdfDocumentFactory.finalizeStructure(
                    baos.toByteArray(), BankPdfDocumentInfo.forSbi(statement));
        } catch (Exception e) {
            log.error("SBI PDF generation failed", e);
            throw new ApiException("SBI PDF generation failed: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @Override
    public String buildDownloadFilename(Statement statement) {
        String account = statement.getAccountDetails() != null
                ? statement.getAccountDetails().getAccountNumber() : "unknown";
        return "SBI_Statement_" + account + ".pdf";
    }

    private void addTopBanner(Document doc, Statement statement, BankPdfImageCache imageCache) {
        CustomerDetails customer = statement.getCustomerDetails();
        String asOnDate = LocalDate.now().format(DATE_DASH);
        String salutation = salutation(customer);
        String fullName = customer != null ? salutation + " " + customer.getCustomerName() : salutation;

        Table banner = new Table(UnitValue.createPercentArray(new float[]{40, 26, 34}))
                .setWidth(UnitValue.createPointValue(SbiPdfStyles.PAGE_WIDTH))
                .setFixedLayout()
                .setMarginLeft(-SbiPdfStyles.MARGIN_LEFT)
                .setMarginRight(-SbiPdfStyles.MARGIN_RIGHT)
                .setMarginBottom(0);

        banner.addCell(bannerLeftCell(imageCache));
        banner.addCell(bannerCenterCell(asOnDate));
        banner.addCell(bannerRightCell(fullName));

        doc.add(banner);
    }

    private Cell bannerLeftCell(BankPdfImageCache imageCache) {
        Cell outer = bannerCell().setPaddingLeft(SbiPdfStyles.BANNER_PAD_LEFT);

        Div stack = new Div();
        float logoWidth = SbiPdfStyles.regular().getWidth("Account Summary", 9f)
                * SbiPdfStyles.BANNER_LOGO_WIDTH_SCALE;
        Image logo = loadBannerLogo(imageCache, logoWidth);
        if (logo != null) {
            stack.add(logo);
        }
        Paragraph summary = text("Account Summary", 9, false, SbiPdfStyles.WHITE)
                .setMarginTop(0.5f)
                .setTextAlignment(TextAlignment.LEFT);
        if (logoWidth > 0f) {
            summary.setMarginLeft(logoWidth * SbiPdfStyles.BANNER_LOGO_TEXT_OFFSET_RATIO);
        }
        stack.add(summary);

        outer.add(stack);
        return outer;
    }

    private Cell bannerCenterCell(String asOnDate) {
        Cell outer = bannerCell().setVerticalAlignment(VerticalAlignment.MIDDLE);

        String dateLabel = "As on " + asOnDate;
        float boxWidth = SbiPdfStyles.BANNER_DATE_BOX_WIDTH;
        float innerWidth = boxWidth - (SbiPdfStyles.BANNER_DATE_BOX_PAD_H * 2f);

        Table inner = new Table(UnitValue.createPointArray(new float[]{12, innerWidth - 14f}))
                .setBorder(Border.NO_BORDER)
                .setWidth(UnitValue.createPointValue(innerWidth))
                .setFixedLayout();

        Cell iconCell = new Cell().setBorder(Border.NO_BORDER).setPadding(0).setVerticalAlignment(VerticalAlignment.MIDDLE);
        iconCell.add(new Paragraph(SbiPdfIcons.CALENDAR)
                .setFont(SbiPdfIcons.font())
                .setFontSize(SbiPdfStyles.FONT_BODY)
                .setFontColor(SbiPdfStyles.WHITE)
                .setMargin(0));

        Cell dateCell = new Cell().setBorder(Border.NO_BORDER).setPadding(0).setPaddingLeft(2)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        dateCell.add(text(dateLabel, SbiPdfStyles.FONT_BODY, false, SbiPdfStyles.WHITE)
                .setKeepTogether(true));

        inner.addCell(iconCell);
        inner.addCell(dateCell);

        Div box = new Div()
                .setWidth(UnitValue.createPointValue(boxWidth))
                .setBorder(new SolidBorder(SbiPdfStyles.WHITE, 0.8f))
                .setBorderRadius(new BorderRadius(8))
                .setPaddingLeft(SbiPdfStyles.BANNER_DATE_BOX_PAD_H)
                .setPaddingRight(SbiPdfStyles.BANNER_DATE_BOX_PAD_H)
                .setPaddingTop(SbiPdfStyles.BANNER_DATE_BOX_PAD_V)
                .setPaddingBottom(SbiPdfStyles.BANNER_DATE_BOX_PAD_V)
                .setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
        box.add(inner);
        outer.add(box);
        return outer;
    }

    private Cell bannerRightCell(String fullName) {
        Cell cell = bannerCell()
                .setTextAlignment(TextAlignment.RIGHT)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPaddingRight(SbiPdfStyles.BANNER_PAD_RIGHT);
        cell.add(text("Welcome:", SbiPdfStyles.FONT_BODY, false, SbiPdfStyles.WHITE));
        cell.add(text(fullName, SbiPdfStyles.BANNER_WELCOME_NAME_SIZE, false, SbiPdfStyles.WHITE).setMarginTop(1));
        return cell;
    }

    private Cell bannerCell() {
        return new Cell()
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(SbiPdfStyles.BANNER_BG)
                .setPaddingTop(SbiPdfStyles.BANNER_PAD_TOP)
                .setPaddingBottom(SbiPdfStyles.BANNER_PAD_BOTTOM)
                .setMinHeight(SbiPdfStyles.BANNER_HEIGHT);
    }

    /** Transparent banner logo (white wordmark + cyan mark, no white box). */
    private Image loadBannerLogo(BankPdfImageCache imageCache, float targetWidth) {
        try {
            Image img = new Image(imageCache.get("pdf/sbiLogoBanner.png"));
            float aspect = img.getImageWidth() / img.getImageHeight();
            img.setWidth(targetWidth);
            img.setHeight(targetWidth / aspect);
            return img;
        } catch (Exception e) {
            log.warn("SBI banner logo not found at classpath:pdf/sbiLogoBanner.png", e);
            return null;
        }
    }

    private void addTitleSection(Document doc) {
        doc.add(new Paragraph("STATEMENT OF ACCOUNT")
                .setFont(SbiPdfStyles.bold())
                .setFontSize(SbiPdfStyles.FONT_TITLE)
                .setFontColor(SbiPdfStyles.TEXT)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(SbiPdfStyles.TITLE_MARGIN_TOP)
                .setMarginBottom(0));
        doc.add(new Div()
                .setHeight(1)
                .setBackgroundColor(SbiPdfStyles.TITLE_LINE)
                .setMarginTop(SbiPdfStyles.TITLE_LINE_MARGIN_TOP)
                .setMarginBottom(SbiPdfStyles.TITLE_LINE_MARGIN_BOTTOM));
    }

    private void addCustomerSection(Document doc, Statement statement) {
        CustomerDetails c = statement.getCustomerDetails();
        AccountDetails a = statement.getAccountDetails();
        String branchCode = deriveBranchCode(a != null ? a.getIfscCode() : null);
        String micr = deriveMicr(a != null ? a.getIfscCode() : null);
        String cif = deriveCif(a != null ? a.getAccountNumber() : null);
        BigDecimal closing = statement.getClosingBalance() != null
                ? statement.getClosingBalance() : statement.getOpeningBalance();
        String salutation = salutation(c);
        String displayName = c != null ? salutation + " " + c.getCustomerName() : "";

        float bodyFont = SbiPdfStyles.FONT_BODY;
        Table section = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth()
                .setMarginTop(SbiPdfStyles.TITLE_TO_CUSTOMER_GAP)
                .setMarginBottom(4);

        Cell left = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPaddingTop(0)
                .setPaddingBottom(0)
                .setPaddingLeft(0)
                .setPaddingRight(SbiPdfStyles.CUSTOMER_COLUMN_INNER_GAP);
        if (c != null) {
            left.add(SbiPdfLayoutHelper.iconTextRow(SbiPdfIcons.USER, displayName, bodyFont));
            left.add(SbiPdfLayoutHelper.iconTextRow(SbiPdfIcons.ENVELOPE, nullToEmpty(c.getEmail()), bodyFont));
            left.add(SbiPdfLayoutHelper.iconTextRow(SbiPdfIcons.LOCATION, buildAddressLine(c), bodyFont));
            left.add(SbiPdfLayoutHelper.iconLabelValueRows(SbiPdfIcons.CALENDAR,
                    new String[][]{{"Date of Statement", LocalDate.now().format(DATE_DASH)}}, bodyFont)
                    .setMarginBottom(SbiPdfStyles.CUSTOMER_SECTION_GAP));
        }
        left.add(SbiPdfLayoutHelper.dividerLabelValueRowsWithMargin(new String[][]{
                {"Clear Balance", formatIndianMoney(closing) + "CR"},
                {"Uncleared Amount", "0.00"},
                {"+MOD Bal", "0.00"},
                {"Lien", "0.0"}
        }, bodyFont, SbiPdfStyles.BALANCE_BLOCK_MARGIN_V, SbiPdfStyles.BALANCE_BLOCK_MARGIN_V));
        left.add(SbiPdfLayoutHelper.iconLabelValueRows(SbiPdfIcons.CREDIT_CARD,
                new String[][]{{"Limit", "0.00"}}, bodyFont));
        left.add(SbiPdfLayoutHelper.dividerLabelValueRows(new String[][]{
                {"Monthly Avg Balance", "0.00"},
                {"Interest Rate", "2.50 % p.a."},
                {"Drawing Power", "0.00"}
        }, bodyFont).setMarginTop(SbiPdfStyles.CUSTOMER_SECTION_GAP));
        left.add(SbiPdfLayoutHelper.iconLabelValueRows(SbiPdfIcons.BANK, new String[][]{
                {"Account open Date", formatDateSlash(statement.getPeriod() != null
                        ? statement.getPeriod().getFromDate().minusYears(4) : LocalDate.now().minusYears(4))}
        }, bodyFont));

        Cell right = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPaddingTop(0)
                .setPaddingBottom(0)
                .setPaddingLeft(SbiPdfStyles.CUSTOMER_COLUMN_INNER_GAP)
                .setPaddingRight(0);
        right.add(new Paragraph("State Bank of India")
                .setFont(SbiPdfStyles.bold())
                .setFontSize(bodyFont)
                .setFontColor(SbiPdfStyles.BANK_TITLE)
                .setMarginBottom(SbiPdfStyles.CUSTOMER_ROW_GAP));
        if (c != null && c.getCity() != null) {
            right.add(new Paragraph(c.getCity().toUpperCase())
                    .setFont(SbiPdfStyles.bold())
                    .setFontSize(bodyFont)
                    .setFontColor(SbiPdfStyles.TEXT)
                    .setMarginBottom(SbiPdfStyles.CUSTOMER_ROW_GAP));
        }
        if (a != null) {
            right.add(SbiPdfLayoutHelper.iconTextRowRight(SbiPdfIcons.LOCATION, buildBranchAddressLine(c, a), bodyFont)
                    .setMarginBottom(SbiPdfStyles.CUSTOMER_SECTION_GAP));
            right.add(SbiPdfLayoutHelper.iconLabelValueRowsRight(SbiPdfIcons.BUILDING, new String[][]{
                    {"Branch Code", branchCode},
                    {"Branch Name", nullToEmpty(a.getBranchName())},
                    {"Branch Email ID", "sbi." + branchCode + "@sbi.co.in"},
                    {"Branch Phone", "8989791932"}
            }, bodyFont).setMarginBottom(SbiPdfStyles.CUSTOMER_SECTION_GAP));
            right.add(SbiPdfLayoutHelper.iconLabelValueRowsRight(SbiPdfIcons.FILE, new String[][]{
                    {"CIF Number", cif},
                    {"Account Number", nullToEmpty(a.getAccountNumber())},
                    {"Product", nullToEmpty(a.getAccountType())},
                    {"IFSC Code", nullToEmpty(a.getIfscCode())},
                    {"Currency", "INR"},
                    {"Account Status", "OPEN"},
                    {"CKYCR Number", "40012745005800"},
                    {"MICR Code", micr}
            }, bodyFont));
            right.add(SbiPdfLayoutHelper.iconLabelValueRowsRight(SbiPdfIcons.USER_PLUS,
                    new String[][]{{"Nominee Name", "XXXXXXXXXXXXXXX"}}, bodyFont));
        }

        section.addCell(left);
        section.addCell(right);
        doc.add(section);
    }

    private String buildAddressLine(CustomerDetails c) {
        StringBuilder sb = new StringBuilder(nullToEmpty(c.getAddress()));
        if (c.getCity() != null) sb.append(", ").append(c.getCity());
        if (c.getPincode() != null) sb.append(",").append(c.getPincode());
        return sb.toString();
    }

    private String buildBranchAddressLine(CustomerDetails c, AccountDetails a) {
        String city = c != null && c.getCity() != null ? c.getCity().toUpperCase() : "";
        String state = c != null && c.getState() != null ? c.getState().toUpperCase().replace(" ", "") : "";
        String branch = a.getBranchName() != null ? a.getBranchName().toUpperCase().replace(" ", "") : "";
        if (!branch.isEmpty() && !city.isEmpty()) {
            return branch + "," + city + "," + state;
        }
        return buildBranchAddress(c, a);
    }

    private void addPeriodLine(Document doc, Statement statement) {
        StatementPeriod period = statement.getPeriod();
        String from = period != null ? formatDateDash(period.getFromDate()) : "";
        String to = period != null ? formatDateDash(period.getToDate()) : "";
        doc.add(new Paragraph("Statement From : " + from + " to " + to)
                .setFont(SbiPdfStyles.regular())
                .setFontSize(SbiPdfStyles.FONT_BODY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(SbiPdfStyles.PERIOD_MARGIN_TOP)
                .setMarginBottom(SbiPdfStyles.PERIOD_MARGIN_BOTTOM));
    }

    private Table buildTransactionTable(Statement statement) {
        List<Transaction> txns = statement.getTransactions();
        String branchCode = deriveBranchCode(
                statement.getAccountDetails() != null ? statement.getAccountDetails().getIfscCode() : null);
        String branchName = statement.getAccountDetails() != null
                ? statement.getAccountDetails().getBranchName() : "";

        Table table = new Table(UnitValue.createPointArray(SbiPdfStyles.TXN_COL_WIDTHS))
                .useAllAvailableWidth()
                .setMarginTop(SbiPdfStyles.PERIOD_TO_TABLE_GAP - SbiPdfStyles.TXN_HEADER_REPEAT_TOP_PAD)
                .setMarginLeft(SbiPdfStyles.TABLE_INSET_H)
                .setMarginRight(SbiPdfStyles.TABLE_INSET_H)
                .setMarginBottom(0);

        addTxnContinuationSpacerRow(table);
        addTxnHeader(table, "Value Date");
        addTxnHeader(table, "Post Date");
        addTxnHeader(table, "Details");
        addTxnHeader(table, "Ref No / Cheque No");
        addTxnHeader(table, "₹ Debit");
        addTxnHeader(table, "₹ Credit");
        addTxnHeader(table, "Balance");
        addTxnFooterStrip(table);

        if (txns != null) {
            for (Transaction txn : txns) {
                String dateStr = formatDateSlash(txn.getDate());
                table.addCell(txnBodyCell(dateStr, TextAlignment.CENTER));
                table.addCell(txnBodyCell(dateStr, TextAlignment.CENTER));
                table.addCell(txnDetailsCell(txn, branchCode, branchName));
                table.addCell(txnBodyCell(refDisplay(txn.getReference()), TextAlignment.CENTER));
                table.addCell(txnAmountCell(TransactionAmountGuard.displayDebit(txn), true));
                table.addCell(txnAmountCell(TransactionAmountGuard.displayCredit(txn), false));
                table.addCell(txnBodyCell(formatIndianMoney(txn.getBalance()), TextAlignment.RIGHT));
            }
        }

        return table;
    }

    private void addTxnContinuationSpacerRow(Table table) {
        table.addHeaderCell(new Cell(1, 7)
                .setBorder(Border.NO_BORDER)
                .setMinHeight(SbiPdfStyles.TXN_HEADER_REPEAT_TOP_PAD)
                .setPadding(0));
    }

    private void addTxnFooterStrip(Table table) {
        for (int i = 0; i < 7; i++) {
            table.addFooterCell(txnFooterStripCell());
        }
    }

    private Cell txnFooterStripCell() {
        return new Cell()
                .setBackgroundColor(SbiPdfStyles.TABLE_FOOTER_STRIP)
                .setBorder(new SolidBorder(SbiPdfStyles.BORDER, 0.5f))
                .setMinHeight(SbiPdfStyles.TXN_FOOTER_ROW_HEIGHT)
                .setHeight(UnitValue.createPointValue(SbiPdfStyles.TXN_FOOTER_ROW_HEIGHT))
                .setPadding(0)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private void addTxnHeader(Table table, String title) {
        table.addHeaderCell(new Cell()
                .add(new Paragraph(title)
                        .setFont(SbiPdfStyles.bold())
                        .setFontSize(SbiPdfStyles.FONT_TABLE)
                        .setFontColor(SbiPdfStyles.WHITE)
                        .setMargin(0))
                .setBackgroundColor(SbiPdfStyles.TABLE_HEADER)
                .setBorder(new SolidBorder(SbiPdfStyles.WHITE, 0.75f))
                .setPaddingTop(SbiPdfStyles.TXN_HEADER_PADDING_TOP)
                .setPaddingBottom(SbiPdfStyles.TXN_HEADER_PADDING_BOTTOM)
                .setPaddingLeft(4)
                .setPaddingRight(4)
                .setMinHeight(SbiPdfStyles.TXN_HEADER_MIN_HEIGHT)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE));
    }

    private Cell txnBodyCell(String text, TextAlignment align) {
        return new Cell()
                .add(new Paragraph(text)
                        .setFont(SbiPdfStyles.regular())
                        .setFontSize(SbiPdfStyles.FONT_TABLE)
                        .setFontColor(SbiPdfStyles.TEXT)
                        .setMultipliedLeading(SbiPdfStyles.TXN_LINE_HEIGHT)
                        .setMargin(0))
                .setBorder(new SolidBorder(SbiPdfStyles.BORDER, 0.5f))
                .setPaddingTop(SbiPdfStyles.TXN_BODY_PAD_V)
                .setPaddingBottom(SbiPdfStyles.TXN_BODY_PAD_V)
                .setPaddingLeft(4)
                .setPaddingRight(4)
                .setMinHeight(SbiPdfStyles.TXN_ROW_MIN_HEIGHT)
                .setTextAlignment(align)
                .setVerticalAlignment(VerticalAlignment.TOP);
    }

    private Cell txnDetailsCell(Transaction txn, String branchCode, String branchName) {
        String typeLine = txnTypeLine(txn);
        String narration = nullToEmpty(txn.getNarration());
        String ref = nullToEmpty(txn.getReference());
        String location = ref + " AT " + branchCode + " " + branchName;

        Paragraph p = new Paragraph()
                .setFont(SbiPdfStyles.regular())
                .setFontSize(SbiPdfStyles.FONT_TABLE)
                .setFontColor(SbiPdfStyles.TEXT)
                .setMultipliedLeading(SbiPdfStyles.TXN_LINE_HEIGHT)
                .setMargin(0);
        p.add(typeLine + "\n");
        p.add(narration + "\n");
        p.add(location);

        return new Cell()
                .add(p)
                .setBorder(new SolidBorder(SbiPdfStyles.BORDER, 0.5f))
                .setPaddingTop(SbiPdfStyles.TXN_BODY_PAD_V)
                .setPaddingBottom(SbiPdfStyles.TXN_BODY_PAD_V)
                .setPaddingLeft(4)
                .setPaddingRight(4)
                .setMinHeight(SbiPdfStyles.TXN_ROW_MIN_HEIGHT)
                .setVerticalAlignment(VerticalAlignment.TOP);
    }

    private Cell txnAmountCell(BigDecimal amount, boolean debit) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return txnBodyCell("", TextAlignment.RIGHT);
        }
        return txnBodyCell(formatIndianMoney(amount.abs()), TextAlignment.RIGHT);
    }

    private void addSummarySection(Document doc, Statement statement) {
        StatementPeriod period = statement.getPeriod();
        String from = period != null ? formatDateDash(period.getFromDate()) : "";
        String to = period != null ? formatDateDash(period.getToDate()) : "";

        Table titleBar = summaryTitleBar("Statement Summary : " + from + " To " + to);
        titleBar.setMarginTop(SbiPdfStyles.SUMMARY_TOP_GAP)
                .setMarginLeft(SbiPdfStyles.SUMMARY_INSET_H)
                .setMarginRight(SbiPdfStyles.SUMMARY_INSET_H);
        doc.add(titleBar);

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        int drCount = 0;
        int crCount = 0;
        if (statement.getTransactions() != null) {
            for (Transaction t : statement.getTransactions()) {
                if (t.getDebit() != null && t.getDebit().compareTo(BigDecimal.ZERO) > 0) {
                    totalDebit = totalDebit.add(t.getDebit());
                    drCount++;
                }
                if (t.getCredit() != null && t.getCredit().compareTo(BigDecimal.ZERO) > 0) {
                    totalCredit = totalCredit.add(t.getCredit());
                    crCount++;
                }
            }
        }

        BigDecimal closing = statement.getClosingBalance() != null
                ? statement.getClosingBalance() : statement.getOpeningBalance();

        Table table = new Table(UnitValue.createPercentArray(new float[]{16.66f, 16.66f, 16.66f, 16.66f, 16.66f, 16.66f}))
                .useAllAvailableWidth()
                .setMarginTop(0)
                .setMarginLeft(SbiPdfStyles.SUMMARY_INSET_H)
                .setMarginRight(SbiPdfStyles.SUMMARY_INSET_H)
                .setMarginBottom(10);

        String[] headers = {
                "Brought Forward (₹)",
                "Dr Count",
                "Cr Count",
                "Total Debits (₹)",
                "Total Credits (₹)",
                "Closing Balance (₹)"
        };
        for (String h : headers) {
            table.addCell(summaryGridHeaderCell(h));
        }

        table.addCell(summaryGridBodyCell(formatIndianMoney(statement.getOpeningBalance()) + "CR"));
        table.addCell(summaryGridBodyCell(String.valueOf(drCount)));
        table.addCell(summaryGridBodyCell(String.valueOf(crCount)));
        table.addCell(summaryGridBodyCell(formatIndianMoney(totalDebit)));
        table.addCell(summaryGridBodyCell(formatIndianMoney(totalCredit)));
        table.addCell(summaryGridBodyCell(formatIndianMoney(closing) + "CR"));

        doc.add(table);
    }

    private Table summaryTitleBar(String title) {
        Table bar = new Table(1).useAllAvailableWidth().setMarginBottom(0);
        bar.addCell(new Cell()
                .setBackgroundColor(SbiPdfStyles.TABLE_HEADER)
                .setBorder(Border.NO_BORDER)
                .setMinHeight(SbiPdfStyles.SUMMARY_BAR_HEIGHT)
                .setPaddingTop(4)
                .setPaddingBottom(4)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .add(new Paragraph(title)
                        .setFont(SbiPdfStyles.bold())
                        .setFontSize(8)
                        .setFontColor(SbiPdfStyles.WHITE)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMargin(0)));
        return bar;
    }

    private String refDisplay(String reference) {
        if (reference == null || reference.isBlank()) {
            return "";
        }
        return reference;
    }

    private Cell summaryGridHeaderCell(String title) {
        return new Cell()
                .add(new Paragraph(title)
                        .setFont(SbiPdfStyles.bold())
                        .setFontSize(7)
                        .setFontColor(SbiPdfStyles.TEXT)
                        .setMargin(0))
                .setBackgroundColor(SbiPdfStyles.WHITE)
                .setBorder(new SolidBorder(SbiPdfStyles.TEXT, 0.5f))
                .setMinHeight(SbiPdfStyles.SUMMARY_HEADER_ROW_HEIGHT)
                .setPaddingTop(4)
                .setPaddingBottom(4)
                .setPaddingLeft(4)
                .setPaddingRight(4)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell summaryGridBodyCell(String value) {
        return new Cell()
                .add(new Paragraph(value)
                        .setFont(SbiPdfStyles.regular())
                        .setFontSize(7)
                        .setFontColor(SbiPdfStyles.TEXT)
                        .setMargin(0))
                .setBackgroundColor(SbiPdfStyles.WHITE)
                .setBorder(new SolidBorder(SbiPdfStyles.TEXT, 0.5f))
                .setMinHeight(SbiPdfStyles.SUMMARY_VALUE_ROW_HEIGHT)
                .setPaddingTop(4)
                .setPaddingBottom(4)
                .setPaddingLeft(4)
                .setPaddingRight(4)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private void addInstructions(Document doc) {
        doc.add(bullet("Please do not share your ATM, Debit/Credit Card number, PIN (Personal Identification number ), OTP (One-Time Password), Username or Password with anyone via email, SMS, phone call, or any other medium. Bank never asks for such information."));
        doc.add(bullet("If your account is operated by a Power of Attorney holder, please review the transactions with extra care."));
        doc.add(bullet("This is a computer generated statement and does not require a signature."));
    }

    private Paragraph bullet(String text) {
        return new Paragraph("• " + text)
                .setFont(SbiPdfStyles.regular())
                .setFontSize(SbiPdfStyles.FONT_BODY)
                .setFontColor(SbiPdfStyles.TEXT)
                .setMarginBottom(2);
    }

    private Paragraph labelValue(String label, String value) {
        return new Paragraph()
                .add(new com.itextpdf.layout.element.Text(label + " : ")
                        .setFont(SbiPdfStyles.bold())
                        .setFontSize(7))
                .add(new com.itextpdf.layout.element.Text(value)
                        .setFont(SbiPdfStyles.regular())
                        .setFontSize(7))
                .setMarginBottom(1.5f);
    }

    private Paragraph text(String content, float size, boolean bold, com.itextpdf.kernel.colors.Color color) {
        return new Paragraph(content)
                .setFont(bold ? SbiPdfStyles.bold() : SbiPdfStyles.regular())
                .setFontSize(size)
                .setFontColor(color)
                .setMargin(0);
    }

    private String txnTypeLine(Transaction txn) {
        if (txn.getCredit() != null && txn.getCredit().compareTo(BigDecimal.ZERO) > 0) {
            return "DEP TFR";
        }
        if (txn.getDebit() != null && txn.getDebit().compareTo(BigDecimal.ZERO) > 0) {
            return "WDL TFR";
        }
        String type = txn.getType();
        if (type != null && type.equalsIgnoreCase("INT")) {
            return "INTEREST CREDIT";
        }
        if (type != null && type.contains("ATM")) {
            return "ATM WDL";
        }
        return "WDL TFR";
    }

    private String salutation(CustomerDetails c) {
        if (c == null || c.getGender() == null) return "Mr.";
        return "female".equalsIgnoreCase(c.getGender()) ? "Mrs." : "Mr.";
    }

    private String buildBranchAddress(CustomerDetails c, AccountDetails a) {
        String city = c != null && c.getCity() != null ? c.getCity().toUpperCase() : "";
        String state = c != null && c.getState() != null ? c.getState().toUpperCase() : "";
        String branch = a.getBranchName() != null ? a.getBranchName().toUpperCase() : "";
        return city + "," + branch + "," + state;
    }

    private String deriveBranchCode(String ifsc) {
        if (ifsc == null || ifsc.length() < 4) return "0000";
        return ifsc.substring(ifsc.length() - 4);
    }

    private String deriveMicr(String ifsc) {
        if (ifsc == null || ifsc.length() < 6) return "000000000";
        return ifsc.substring(ifsc.length() - 6) + "042";
    }

    private String deriveCif(String accountNumber) {
        if (accountNumber == null || accountNumber.isEmpty()) return "00000000000";
        return accountNumber.length() > 11 ? accountNumber.substring(0, 11) : accountNumber;
    }

    private String formatDateSlash(LocalDate date) {
        return date == null ? "" : date.format(DATE_SLASH);
    }

    private String formatDateDash(LocalDate date) {
        return date == null ? "" : date.format(DATE_DASH);
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private String formatIndianMoney(BigDecimal amount) {
        if (amount == null) return "0.00";
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
}
