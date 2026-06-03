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
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.AreaBreakType;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
public class KotakPdfService implements BankPdfService {

    private static final DateTimeFormatter KOTAK_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter GENERATED_ON = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);

    @Override
    public boolean supports(String bankCode) {
        return "KOTAK".equalsIgnoreCase(bankCode);
    }

    @Override
    public byte[] generate(Statement statement, boolean includeWatermark) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            KotakPdfStyles.clearCache();
            KotakPdfIcons.clearCache();
            KotakPdfImageCache imageCache = new KotakPdfImageCache();

            PdfWriter writer = BankPdfDocumentFactory.createWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);

            KotakPageContext pageContext = buildPageContext(statement);
            log.info(
                    "Kotak PDF generated bankCode=KOTAK accountHolderName=\"{}\"",
                    pageContext.getCustomerName());
            KotakPageEventHandler handler = new KotakPageEventHandler(pageContext);
            pdf.addEventHandler(PdfDocumentEvent.START_PAGE, handler);

            Document doc = new Document(pdf, PageSize.A4);
            doc.setMargins(KotakPdfStyles.MARGIN_TOP, KotakPdfStyles.MARGIN_RIGHT,
                    KotakPdfStyles.MARGIN_BOTTOM, KotakPdfStyles.MARGIN_LEFT);
            handler.bindDocument(doc);

            addLogoRow(doc, imageCache);
            addTitleSection(doc, statement);
            addCustomerAccountSection(doc, statement);
            doc.add(buildTransactionTable(statement));
            handler.disableContinuationHeader();
            addEndAndSummarySections(doc, statement, imageCache);
            addImportantInformation(doc, statement);

            doc.close();

            byte[] draft = baos.toByteArray();
            return BankPdfDocumentFactory.finalizeKotakStructure(draft, document -> {
                KotakPdfStyles.clearCache();
                KotakPdfIcons.clearCache();
                new KotakPageEventHandler(pageContext).applyFooters(document);
            });
        } catch (Exception e) {
            log.error("Kotak PDF generation failed", e);
            throw new ApiException("Kotak PDF generation failed: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @Override
    public byte[] generateForDownload(Statement statement) {
        byte[] pdf = generate(statement, false);
        log.info(
                "Kotak Download PDF generated (POST /api/statements/{}/download) statementId={} sizeBytes={} sizeKb={}",
                statement.getId(),
                statement.getId(),
                pdf.length,
                Math.round(pdf.length / 1024.0));
        return pdf;
    }

    @Override
    public String buildDownloadFilename(Statement statement) {
        String account = statement.getAccountDetails() != null
                ? statement.getAccountDetails().getAccountNumber() : "unknown";
        return "KOTAK_Statement_" + account + ".pdf";
    }

    private KotakPageContext buildPageContext(Statement statement) {
        CustomerDetails c = statement.getCustomerDetails();
        AccountDetails a = statement.getAccountDetails();
        StatementPeriod p = statement.getPeriod();
        String period = formatPeriodRange(p);
        return KotakPageContext.builder()
                .customerName(normalizeCustomerName(c != null ? c.getCustomerName() : null))
                .accountNumber(a != null ? nullToEmpty(a.getAccountNumber()) : "")
                .periodRange(period)
                .generatedOn(LocalDateTime.now().format(GENERATED_ON))
                .build();
    }

    private String resolveTransactionSectionTitle(Statement statement) {
        String accountType = statement.getAccountDetails() != null
                ? statement.getAccountDetails().getAccountType() : "Savings";
        return accountType + " Account Transactions";
    }

    private void addLogoRow(Document doc, KotakPdfImageCache imageCache) {
        Table logos = new Table(UnitValue.createPercentArray(new float[]{38, 24, 38}))
                .useAllAvailableWidth()
                .setBorder(Border.NO_BORDER)
                .setMarginTop(KotakPdfStyles.LOGO_TOP_PADDING)
                .setMarginBottom(KotakPdfStyles.LOGO_ROW_MARGIN_BOTTOM);

        Cell left = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(0)
                .setVerticalAlignment(VerticalAlignment.BOTTOM);
        Image leftImg = loadImage(imageCache, "pdf/kotak/image-left.png", KotakPdfStyles.LOGO_LEFT_HEIGHT);
        if (leftImg != null) {
            leftImg.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.LEFT);
            left.add(leftImg);
        }

        Cell gutter = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(0);

        Cell right = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(0)
                .setTextAlignment(TextAlignment.RIGHT)
                .setVerticalAlignment(VerticalAlignment.BOTTOM);
        Image rightImg = loadImage(imageCache, "pdf/kotak/kotak-logo.png", KotakPdfStyles.LOGO_RIGHT_HEIGHT);
        if (rightImg != null) {
            rightImg.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.RIGHT);
            right.add(rightImg);
        }

        logos.addCell(left);
        logos.addCell(gutter);
        logos.addCell(right);
        doc.add(logos);
    }

    private void addTitleSection(Document doc, Statement statement) {
        StatementPeriod period = statement.getPeriod();
        doc.add(new Paragraph("Account Statement")
                .setFont(KotakPdfStyles.bold())
                .setFontSize(KotakPdfStyles.FONT_TITLE)
                .setFontColor(KotakPdfStyles.TEXT_DARK)
                .setMarginBottom(KotakPdfStyles.TITLE_TO_DATE_GAP));
        doc.add(new Paragraph(formatPeriodRange(period))
                .setFont(KotakPdfStyles.regular())
                .setFontSize(KotakPdfStyles.FONT_DATE_RANGE)
                .setFontColor(KotakPdfStyles.TEXT_MUTED)
                .setMarginBottom(10));
    }

    private void addCustomerAccountSection(Document doc, Statement statement) {
        CustomerDetails c = statement.getCustomerDetails();
        AccountDetails a = statement.getAccountDetails();
        String micr = deriveMicr(a != null ? a.getIfscCode() : null);
        String ifsc = a != null ? nullToEmpty(a.getIfscCode()) : "";

        float halfGap = KotakPdfStyles.CUSTOMER_COLUMN_GAP / 2f;
        Table section = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth()
                .setMarginBottom(KotakPdfStyles.TXN_SECTION_TOP_GAP);

        Cell left = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(0)
                .setPaddingRight(halfGap);
        if (c != null) {
            String customerName = normalizeCustomerName(c.getCustomerName());
            left.add(textLine(customerName, KotakPdfStyles.FONT_BODY + 2.5f, true)
                    .setMarginBottom(2));
            left.add(textLine("CRN " + deriveCrn(a), KotakPdfStyles.FONT_BODY - 0.5f, false)
                    .setFontColor(KotakPdfStyles.TEXT_MUTED)
                    .setMarginBottom(6));
            left.add(addressLine(nullToEmpty(c.getAddress())));
            if (c.getCity() != null) {
                left.add(addressLine(c.getCity() + " - " + nullToEmpty(c.getPincode())));
            }
            String stateCountry = (nullToEmpty(c.getState()) + " - India").trim();
            if (!stateCountry.equals("- India")) {
                left.add(addressLine(stateCountry));
            }
            left.add(micrIfscLine(micr, ifsc).setMarginTop(6));
        }

        Cell right = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(0)
                .setPaddingLeft(halfGap);
        if (a != null) {
            // Group 1
            right.add(labelValueLine("Account No.", a.getAccountNumber(), true, 3));
            right.add(labelValueLine("Account Type", a.getAccountType(), false, 3));
            right.add(labelValueLine("Branch", a.getBranchName(), false, 8));
            // Group 2
            right.add(labelValueLine("Branch Phone Number", "9522262613", false, 10));
            // Group 3
            right.add(labelValueLine("Account Status", "Active", false, 3));
            right.add(labelValueLine("Nominee Registered", "No", false, 10));
            // Group 4
            right.add(labelValueLine("Currency", "INDIAN RUPEE", false, 0));
        }

        section.addCell(left);
        section.addCell(right);
        doc.add(section);
    }

    private Table buildTransactionTable(Statement statement) {
        String sectionTitle = resolveTransactionSectionTitle(statement);
        Table table = new Table(UnitValue.createPointArray(KotakPdfStyles.TXN_COL_WIDTHS))
                .useAllAvailableWidth();

        addTxnRedBarHeader(table, sectionTitle);
        addTxnHeader(table, "#");
        addTxnHeader(table, "Date");
        addTxnHeader(table, "Description");
        addTxnHeader(table, "Chq/Ref. No.");
        addTxnHeader(table, "Withdrawal (Dr.)");
        addTxnHeader(table, "Deposit (Cr.)");
        addTxnHeader(table, "Balance");

        BigDecimal opening = statement.getOpeningBalance() != null
                ? statement.getOpeningBalance() : BigDecimal.ZERO;
        addOpeningRow(table, formatAmount(opening));

        List<Transaction> txns = statement.getTransactions();
        if (txns != null) {
            int serial = 1;
            for (Transaction txn : txns) {
                table.addCell(bodyCell(String.valueOf(serial++), TextAlignment.CENTER));
                table.addCell(bodyCell(formatTxnDate(txn.getDate()), TextAlignment.CENTER));
                table.addCell(bodyCellMultiline(nullToEmpty(txn.getNarration())));
                table.addCell(bodyCell(nullToEmpty(txn.getReference()), TextAlignment.LEFT));
                table.addCell(bodyCell(formatDr(TransactionAmountGuard.displayDebit(txn)), TextAlignment.RIGHT));
                table.addCell(bodyCell(formatCr(TransactionAmountGuard.displayCredit(txn)), TextAlignment.RIGHT));
                table.addCell(bodyCell(formatAmount(txn.getBalance()), TextAlignment.RIGHT));
            }
        }

        return table;
    }

    private void addOpeningRow(Table table, String balance) {
        table.addCell(bodyCell("-", TextAlignment.CENTER));
        table.addCell(bodyCell("-", TextAlignment.CENTER));
        table.addCell(openingBalanceCell("Opening Balance"));
        table.addCell(bodyCell("-", TextAlignment.LEFT));
        table.addCell(bodyCell("-", TextAlignment.RIGHT));
        table.addCell(bodyCell("-", TextAlignment.RIGHT));
        table.addCell(bodyCell(balance, TextAlignment.RIGHT));
    }

    private void addEndAndSummarySections(Document doc, Statement statement, KotakPdfImageCache imageCache) {
        doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

        addSummaryPageCustomerHeader(doc, statement);

        BigDecimal opening = statement.getOpeningBalance() != null
                ? statement.getOpeningBalance() : BigDecimal.ZERO;
        BigDecimal closing = statement.getClosingBalance() != null
                ? statement.getClosingBalance() : opening;

        doc.add(withSummaryInset(redSectionBar("Account Summary")));

        Table summary = withSummaryInset(new Table(UnitValue.createPercentArray(new float[]{50, 25, 25}))
                .useAllAvailableWidth()
                .setMarginBottom(14));
        summary.addHeaderCell(grayHeaderCell("Particulars", TextAlignment.LEFT));
        summary.addHeaderCell(grayHeaderCell("Opening Balance", TextAlignment.CENTER));
        summary.addHeaderCell(grayHeaderCell("Closing Balance", TextAlignment.CENTER));
        summary.addCell(summaryBody("Savings Account (SA):", TextAlignment.LEFT));
        summary.addCell(summaryBody(formatAmount(opening), TextAlignment.RIGHT));
        summary.addCell(summaryBody(formatAmount(closing), TextAlignment.RIGHT));
        doc.add(summary);

        doc.add(withSummaryInset(new Paragraph("End of Statement")
                .setFont(KotakPdfStyles.bold())
                .setFontSize(10)
                .setFontColor(KotakPdfStyles.TEXT_DARK)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10)));

        doc.add(withSummaryInset(endOfStatementLine(
                "Any discrepancy in the statement should be brought to the notice of Kotak Mahindra Bank Ltd. within")));
        doc.add(withSummaryInset(endOfStatementLine(
                "one month from the date of receipt of the statement.")));
        doc.add(withSummaryInset(endOfStatementLine(
                "This is a system generated report and does not require signature and stamp.")
                .setMarginBottom(0)));

        addAdKotakBanner(doc, imageCache);

        doc.add(withSummaryInset(redSectionBar("For assistance, reach out to us at:")));
        doc.add(withSummaryInset(buildContactSection(statement)));
        doc.add(withSummaryInset(buildRememberSection()));

        doc.add(withSummaryInset(new Paragraph("Kotak Mahindra Bank Ltd. | CIN: L65110MH1985PLC038137")
                .setFont(KotakPdfStyles.regular())
                .setFontSize(6.5f)
                .setFontColor(KotakPdfStyles.TEXT_DARK)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10)));
        doc.add(withSummaryInset(new Paragraph(
                "Registered Office: 27 BKC, C 27, G Block, Bandra Kurla Complex, Bandra (E), Mumbai - 400 051. www.kotak.bank.in")
                .setFont(KotakPdfStyles.regular())
                .setFontSize(6.5f)
                .setFontColor(KotakPdfStyles.TEXT_DARK)
                .setTextAlignment(TextAlignment.CENTER)));
    }

    private void addImportantInformation(Document doc, Statement statement) {
        doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

        addSummaryPageCustomerHeader(doc, statement);

        doc.add(withSummaryInset(redSectionBar("Important Information")));
        doc.add(buildImportantInfoBox());

        doc.add(withSummaryInset(redSectionBar("Commonly Used Narrations").setMarginTop(12)));

        String[][] narrations = {
                {"AP - Autopay for Billpay", "Netcard - Netc@rd transaction"},
                {"ATL - ATM withdrawal done from other bank ATM machine", "OS - Online Shopping transaction"},
                {"ATW - ATM withdrawal done from Kotak ATM machine", "OT - Online Trading transaction via Payment Gateway"},
                {"BP - Bill Pay transaction", "PB - Transaction done through Phone Banking (IVR)"},
                {"CDM - Kotak Cash Deposit Machine", "PCI/PCD - POS transaction"},
                {"CMS - Cash Management Service", "RTGS - Real Time Gross Settlement"},
                {"IB - Transaction done on Kotak Net Banking", "UPI - Unified Payment Interface"},
                {"IMPS - Immediate Payment Service", "VISACCPAY - Visa Credit Card Payment"},
                {"IMT - Instant Money Transfer", "VMT - VISA Money Transfer"},
                {"KB - Billpay transaction via Keya Chatbot", "WB - Billpay transaction via WhatsApp Banking"},
                {"MB - Transaction done on Mobile banking", "Int. Pd. - Interest credited on your savings account balance"},
                {"NACH - National Automated Clearing House", "Sweep transfer to - Booking new Term Deposit"},
                {"NEFT - National Electronic Funds Transfer", "Sweep transfer from - Broken existing Term Deposit"}
        };

        Table narrTable = withSummaryInset(new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth()
                .setBorder(new SolidBorder(KotakPdfStyles.BORDER, 0.5f)));
        for (String[] row : narrations) {
            narrTable.addCell(narrCell(row[0]));
            narrTable.addCell(narrCell(row[1]));
        }
        doc.add(narrTable);
    }

    private <T extends com.itextpdf.layout.element.IBlockElement> T withSummaryInset(T element) {
        if (element instanceof Table table) {
            table.setMarginLeft(KotakPdfStyles.SUMMARY_INSET_H);
            table.setMarginRight(KotakPdfStyles.SUMMARY_INSET_H);
        } else if (element instanceof Div div) {
            div.setMarginLeft(KotakPdfStyles.SUMMARY_INSET_H);
            div.setMarginRight(KotakPdfStyles.SUMMARY_INSET_H);
        } else if (element instanceof Paragraph para) {
            para.setMarginLeft(KotakPdfStyles.SUMMARY_INSET_H);
            para.setMarginRight(KotakPdfStyles.SUMMARY_INSET_H);
        }
        return element;
    }

    private Table redSectionBar(String title) {
        Table bar = new Table(1).useAllAvailableWidth().setMarginBottom(0);
        bar.addCell(new Cell()
                .setBackgroundColor(KotakPdfStyles.KOTAK_RED)
                .setBorder(Border.NO_BORDER)
                .setMinHeight(KotakPdfStyles.RED_BAR_HEIGHT)
                .setPaddingTop(6)
                .setPaddingBottom(6)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .add(new Paragraph(title)
                        .setFont(KotakPdfStyles.bold())
                        .setFontSize(9)
                        .setFontColor(KotakPdfStyles.WHITE)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMargin(0)));
        return bar;
    }

    private Cell grayHeaderCell(String text, TextAlignment align) {
        return new Cell()
                .add(new Paragraph(text).setFont(KotakPdfStyles.bold()).setFontSize(8).setFontColor(KotakPdfStyles.WHITE))
                .setBackgroundColor(KotakPdfStyles.HEADER_GRAY)
                .setBorder(new SolidBorder(KotakPdfStyles.WHITE, 0.75f))
                .setPadding(5)
                .setTextAlignment(align)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Table buildContactSection(Statement statement) {
        Table contact = new Table(UnitValue.createPercentArray(new float[]{49.5f, 1f, 49.5f}))
                .useAllAvailableWidth()
                .setMarginTop(12)
                .setMarginBottom(14);

        Cell left = new Cell().setBorder(Border.NO_BORDER).setPadding(12).setTextAlignment(TextAlignment.CENTER);
        left.add(iconParagraph(KotakPdfIcons.PHONE));
        left.add(new Paragraph("Contact Us").setFont(KotakPdfStyles.bold()).setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(6).setMarginBottom(4));
        left.add(new Paragraph("1800 4100").setFont(KotakPdfStyles.bold()).setFontSize(9)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(2));
        left.add(new Paragraph("(Toll-free number)").setFont(KotakPdfStyles.regular()).setFontSize(7)
                .setTextAlignment(TextAlignment.CENTER).setMargin(0));

        Cell divider = new Cell().setBorder(Border.NO_BORDER)
                .setBorderLeft(new SolidBorder(KotakPdfStyles.BORDER, 0.5f))
                .setPadding(0);

        Cell right = new Cell().setBorder(Border.NO_BORDER).setPadding(12).setTextAlignment(TextAlignment.CENTER);
        right.add(iconParagraph(KotakPdfIcons.BRANCH));
        right.add(new Paragraph("Branch Address").setFont(KotakPdfStyles.bold()).setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(6).setMarginBottom(4));
        right.add(new Paragraph(buildBranchAddress(statement)).setFont(KotakPdfStyles.regular()).setFontSize(7)
                .setTextAlignment(TextAlignment.CENTER).setMargin(0).setMultipliedLeading(KotakPdfStyles.INFO_LINE_HEIGHT));

        contact.addCell(left);
        contact.addCell(divider);
        contact.addCell(right);
        return contact;
    }

    private Paragraph iconParagraph(String icon) {
        return new Paragraph(icon)
                .setFont(KotakPdfIcons.font())
                .setFontSize(18)
                .setFontColor(KotakPdfStyles.ICON_BLUE)
                .setTextAlignment(TextAlignment.CENTER)
                .setMargin(0);
    }

    private Table buildRememberSection() {
        Table box = new Table(UnitValue.createPercentArray(new float[]{62, 38}))
                .useAllAvailableWidth()
                .setBackgroundColor(KotakPdfStyles.BOX_BG)
                .setBorder(new SolidBorder(KotakPdfStyles.BORDER, 0.5f))
                .setBorderRadius(new BorderRadius(8));

        Cell left = new Cell().setBorder(Border.NO_BORDER).setPadding(12);
        left.add(new Paragraph("Remember!")
                .setFont(KotakPdfStyles.bold())
                .setFontSize(9)
                .setFontColor(KotakPdfStyles.KOTAK_RED)
                .setMarginBottom(4));
        left.add(new Paragraph(
                "Never share personal/sensitive information like PIN, CVV, OTP or passwords with anyone.")
                .setFont(KotakPdfStyles.regular())
                .setFontSize(7.5f)
                .setFontColor(KotakPdfStyles.TEXT_DARK)
                .setMargin(0));

        Cell right = new Cell().setBorder(Border.NO_BORDER)
                .setBorderLeft(new SolidBorder(KotakPdfStyles.BORDER, 0.5f))
                .setPadding(10)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        Table qrRow = new Table(UnitValue.createPercentArray(new float[]{40, 60}))
                .useAllAvailableWidth()
                .setBorder(Border.NO_BORDER);
        Cell qrCell = new Cell().setBorder(Border.NO_BORDER);
        Image qrImage = loadKotakQrCodeImage();
        if (qrImage != null) {
            qrCell.add(qrImage);
        }
        qrRow.addCell(qrCell);
        qrRow.addCell(new Cell().setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE)
                .add(new Paragraph("Scan for more safe banking tips")
                        .setFont(KotakPdfStyles.regular())
                        .setFontSize(7)
                        .setFontColor(KotakPdfStyles.TEXT_DARK)
                        .setMargin(0)));
        right.add(qrRow);

        box.addCell(left);
        box.addCell(right);
        return box;
    }

    private Paragraph endOfStatementLine(String text) {
        return new Paragraph(text)
                .setFont(KotakPdfStyles.regular())
                .setFontSize(7.5f)
                .setFontColor(KotakPdfStyles.TEXT_DARK)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(2);
    }

    private Image loadKotakQrCodeImage() {
        try (InputStream in = new ClassPathResource("pdf/kotak/KotakQrCode.png").getInputStream()) {
            byte[] optimized = KotakPdfImageOptimizer.toJpeg(
                    KotakPdfImageOptimizer.resizeToExact(in.readAllBytes(), 120, 120), 0.92f);
            Image qr = new Image(ImageDataFactory.create(optimized));
            qr.setWidth(KotakPdfStyles.QR_CODE_SIZE);
            qr.setHeight(KotakPdfStyles.QR_CODE_SIZE);
            return qr;
        } catch (Exception e) {
            log.warn("Kotak QR code image not found", e);
            return null;
        }
    }

    private Div buildImportantInfoBox() {
        Div box = new Div()
                .setBackgroundColor(KotakPdfStyles.WHITE)
                .setBorder(new SolidBorder(KotakPdfStyles.INFO_BOX_BORDER, 1f))
                .setBorderRadius(new BorderRadius(KotakPdfStyles.INFO_BOX_RADIUS))
                .setPaddingTop(KotakPdfStyles.INFO_BOX_PAD_TOP)
                .setPaddingBottom(KotakPdfStyles.INFO_BOX_PAD_BOTTOM)
                .setPaddingLeft(KotakPdfStyles.INFO_BOX_PAD_LEFT)
                .setPaddingRight(KotakPdfStyles.INFO_BOX_PAD_RIGHT)
                .setMarginTop(KotakPdfStyles.INFO_BOX_TOP_GAP)
                .setMarginLeft(KotakPdfStyles.SUMMARY_INSET_H + KotakPdfStyles.INFO_BOX_INSET_H)
                .setMarginRight(KotakPdfStyles.SUMMARY_INSET_H + KotakPdfStyles.INFO_BOX_INSET_H)
                .setMarginBottom(12);

        String[] bullets = {
                "RBI mandates Positive Pay for high-value cheques from Jan 1, 2021. Customers must submit cheque details via Net/Mobile Banking or at the branch on the day of issuance or before handing it to the beneficiary. For more details, visit www.kotak.bank.in.",
                "From October 4, 2025, same-day cheque clearing will be implemented across all banks. Cheques will be credited or debited within a few hours of issuance.",
                "Complimentary insurance cover on Kotak Debit Cards (linked to Saving and Current accounts) will be discontinued w.e.f. July 20, 2025.",
                "In order to avail TDS exemption (if eligible) on existing/new Fixed Deposits for the Financial Year 2023–24, fresh Form 15G (15H for senior citizens) must be submitted at the earliest.",
                "RBI, vide its circular DOR.CRE.REC.23/21.08.008/2022-23 dated April 19, 2022, has issued guidelines pertaining to the opening and maintenance of Current Account(s) of customers who have availed various credit facilities from the banking system.",
                "Deposits of up to ₹5,00,000 per depositor are fully insured by the Deposit Insurance and Credit Guarantee Corporation, under the Deposit Insurance Scheme.",
                "Keep your account active for uninterrupted access to your funds: Inoperative accounts can be easily reactivated by submitting a signed request along with valid KYC documents.",
                "Registering a nominee is strongly recommended: A nominee can help your family access funds lying in your inoperative account smoothly.",
                "Goods and Services Tax (GST), at the applicable rate of 18%, is levied on relevant service charges.",
                "Please note: This statement/ advice should not be construed as a Tax Invoice under the Goods and Services Tax Act."
        };
        for (int i = 0; i < bullets.length; i++) {
            box.add(infoBulletRow(bullets[i], i < bullets.length - 1));
        }
        return box;
    }

    private Table infoBulletRow(String text, boolean gapAfter) {
        Table row = new Table(UnitValue.createPointArray(new float[]{
                KotakPdfStyles.INFO_BULLET_SIZE, 400f}))
                .useAllAvailableWidth()
                .setBorder(Border.NO_BORDER);
        if (gapAfter) {
            row.setMarginBottom(KotakPdfStyles.INFO_ITEM_GAP);
        }

        Cell bulletCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(0)
                .setPaddingRight(KotakPdfStyles.INFO_BULLET_TEXT_GAP)
                .setPaddingTop(2f)
                .setVerticalAlignment(VerticalAlignment.TOP);
        bulletCell.add(new Div()
                .setWidth(UnitValue.createPointValue(KotakPdfStyles.INFO_BULLET_SIZE))
                .setHeight(UnitValue.createPointValue(KotakPdfStyles.INFO_BULLET_SIZE))
                .setBackgroundColor(KotakPdfStyles.INFO_BULLET)
                .setBorderRadius(new BorderRadius(KotakPdfStyles.INFO_BULLET_SIZE / 2f)));

        Cell textCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(0)
                .setVerticalAlignment(VerticalAlignment.TOP);
        textCell.add(new Paragraph(text)
                .setFont(KotakPdfStyles.regular())
                .setFontSize(KotakPdfStyles.FONT_BODY)
                .setFontColor(KotakPdfStyles.INFO_TEXT)
                .setMultipliedLeading(KotakPdfStyles.INFO_LINE_HEIGHT)
                .setMargin(0));

        row.addCell(bulletCell);
        row.addCell(textCell);
        return row;
    }

    private Cell narrCell(String text) {
        return new Cell()
                .setBorder(new SolidBorder(KotakPdfStyles.BORDER, 0.5f))
                .setPadding(4)
                .add(new Paragraph(text).setFont(KotakPdfStyles.regular()).setFontSize(6.5f).setMargin(0));
    }

    private void addTxnRedBarHeader(Table table, String title) {
        table.addHeaderCell(new Cell(1, 7)
                .setBackgroundColor(KotakPdfStyles.KOTAK_RED)
                .setBorder(Border.NO_BORDER)
                .setMinHeight(KotakPdfStyles.RED_BAR_HEIGHT)
                .setPaddingTop(6)
                .setPaddingBottom(6)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .add(new Paragraph(title)
                        .setFont(KotakPdfStyles.bold())
                        .setFontSize(9)
                        .setFontColor(KotakPdfStyles.WHITE)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMargin(0)));
    }

    private void addTxnHeader(Table table, String title) {
        table.addHeaderCell(new Cell()
                .add(new Paragraph(title)
                        .setFont(KotakPdfStyles.bold())
                        .setFontSize(KotakPdfStyles.FONT_TABLE_HEADER)
                        .setFontColor(KotakPdfStyles.WHITE)
                        .setMargin(0))
                .setBackgroundColor(KotakPdfStyles.HEADER_GRAY)
                .setBorder(new SolidBorder(KotakPdfStyles.WHITE, 0.5f))
                .setMinHeight(KotakPdfStyles.TXN_HEADER_HEIGHT)
                .setPaddingTop(5)
                .setPaddingBottom(5)
                .setPaddingLeft(KotakPdfStyles.TXN_CELL_PAD_H)
                .setPaddingRight(KotakPdfStyles.TXN_CELL_PAD_H)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE));
    }

    private Cell openingBalanceCell(String label) {
        return applyTxnBodyCellStyle(new Cell()
                .add(new Paragraph(label)
                        .setFont(KotakPdfStyles.bold())
                        .setFontSize(KotakPdfStyles.FONT_TABLE)
                        .setFontColor(KotakPdfStyles.TEXT_DARK)
                        .setMargin(0)))
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell bodyCell(String text, TextAlignment align) {
        String value = text == null ? "" : text;
        return applyTxnBodyCellStyle(new Cell()
                .add(new Paragraph(value)
                        .setFont(KotakPdfStyles.regular())
                        .setFontSize(KotakPdfStyles.FONT_TABLE)
                        .setFontColor(KotakPdfStyles.TEXT_DARK)
                        .setMargin(0)
                        .setMultipliedLeading(KotakPdfStyles.TXN_ROW_LEADING)))
                .setTextAlignment(align)
                .setVerticalAlignment(VerticalAlignment.TOP);
    }

    /** Horizontal row separators only — vertical column lines are limited to the header row. */
    private Cell applyTxnBodyCellStyle(Cell cell) {
        SolidBorder horizontal = new SolidBorder(KotakPdfStyles.BORDER, 0.5f);
        return cell
                .setBorderTop(horizontal)
                .setBorderBottom(horizontal)
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER)
                .setMinHeight(KotakPdfStyles.TXN_ROW_MIN_HEIGHT)
                .setPaddingTop(KotakPdfStyles.TXN_CELL_PAD_V)
                .setPaddingBottom(KotakPdfStyles.TXN_CELL_PAD_V)
                .setPaddingLeft(KotakPdfStyles.TXN_CELL_PAD_H)
                .setPaddingRight(KotakPdfStyles.TXN_CELL_PAD_H);
    }

    private Cell bodyCellMultiline(String text) {
        String value = text == null ? "" : text;
        return applyTxnBodyCellStyle(new Cell()
                .add(new Paragraph(value)
                        .setFont(KotakPdfStyles.regular())
                        .setFontSize(KotakPdfStyles.FONT_TABLE)
                        .setFontColor(KotakPdfStyles.TEXT_DARK)
                        .setMargin(0)
                        .setMultipliedLeading(KotakPdfStyles.TXN_ROW_LEADING)))
                .setTextAlignment(TextAlignment.LEFT)
                .setVerticalAlignment(VerticalAlignment.TOP);
    }

    private Cell summaryBody(String text, TextAlignment align) {
        return new Cell()
                .add(new Paragraph(text).setFont(KotakPdfStyles.regular()).setFontSize(8).setFontColor(KotakPdfStyles.TEXT_DARK))
                .setBorder(new SolidBorder(KotakPdfStyles.BORDER, 0.5f))
                .setPadding(5)
                .setTextAlignment(align);
    }

    private Paragraph textLine(String text, float size, boolean bold) {
        return new Paragraph(text)
                .setFont(bold ? KotakPdfStyles.bold() : KotakPdfStyles.regular())
                .setFontSize(size)
                .setFontColor(KotakPdfStyles.TEXT_DARK)
                .setMarginBottom(KotakPdfStyles.CUSTOMER_ROW_GAP);
    }

    private Paragraph labelValueLine(String label, String value, boolean boldValue, float marginBottom) {
        return new Paragraph()
                .add(new Text(label + " ")
                        .setFont(KotakPdfStyles.regular())
                        .setFontSize(KotakPdfStyles.FONT_BODY)
                        .setFontColor(KotakPdfStyles.TEXT_MUTED))
                .add(new Text(nullToEmpty(value))
                        .setFont(boldValue ? KotakPdfStyles.bold() : KotakPdfStyles.regular())
                        .setFontSize(KotakPdfStyles.FONT_BODY)
                        .setFontColor(KotakPdfStyles.TEXT_DARK))
                .setMarginBottom(marginBottom)
                .setPaddingLeft(KotakPdfStyles.LABEL_VALUE_GAP);
    }

    private Paragraph addressLine(String text) {
        return new Paragraph(nullToEmpty(text))
                .setFont(KotakPdfStyles.regular())
                .setFontSize(KotakPdfStyles.FONT_BODY)
                .setFontColor(KotakPdfStyles.TEXT_MUTED)
                .setMarginBottom(2)
                .setMultipliedLeading(1.15f);
    }

    // private Paragraph micrIfscLine(String micr, String ifsc) {
    //     return new Paragraph()
    //             .add(new Text("MICR " + nullToEmpty(micr) + " IFSC Code ")
    //                     .setFont(KotakPdfStyles.regular())
    //                     .setFontSize(KotakPdfStyles.FONT_BODY)
    //                     .setFontColor(KotakPdfStyles.TEXT_MUTED))
    //             .add(new Text(nullToEmpty(ifsc))
    //                     .setFont(KotakPdfStyles.regular())
    //                     .setFontSize(KotakPdfStyles.FONT_BODY)
    //                     .setFontColor(KotakPdfStyles.TEXT_DARK))
    //             .setMarginBottom(0);
    // }

    private Paragraph micrIfscLine(String micr, String ifsc) {
    return new Paragraph()

            // MICR label
            .add(new Text("MICR ")
                    .setFont(KotakPdfStyles.regular())
                    .setFontSize(KotakPdfStyles.FONT_BODY)
                    .setFontColor(KotakPdfStyles.TEXT_MUTED))

            // MICR value
            .add(new Text(nullToEmpty(micr))
                    .setFont(KotakPdfStyles.bold())
                    .setFontSize(KotakPdfStyles.FONT_BODY)
                    .setFontColor(KotakPdfStyles.TEXT_DARK))

            // IFSC label
            .add(new Text("  IFSC Code ")
                    .setFont(KotakPdfStyles.regular())
                    .setFontSize(KotakPdfStyles.FONT_BODY)
                    .setFontColor(KotakPdfStyles.TEXT_MUTED))

            // IFSC value
            .add(new Text(nullToEmpty(ifsc))
                    .setFont(KotakPdfStyles.bold())
                    .setFontSize(KotakPdfStyles.FONT_BODY)
                    .setFontColor(KotakPdfStyles.TEXT_DARK))

            .setMarginBottom(0);
}

    private void addSummaryPageCustomerHeader(Document doc, Statement statement) {
        CustomerDetails customer = statement.getCustomerDetails();
        AccountDetails account = statement.getAccountDetails();
        String customerName = normalizeCustomerName(customer != null ? customer.getCustomerName() : null);
        String accountNumber = account != null ? nullToEmpty(account.getAccountNumber()) : "";
        String periodRange = formatPeriodRange(statement.getPeriod());

        Div header = new Div()
                .setMarginBottom(KotakPdfStyles.SUMMARY_PAGE_HEADER_BOTTOM_GAP);

        if (!customerName.isBlank()) {
            header.add(new Paragraph(customerName)
                    .setFont(KotakPdfStyles.bold())
                    .setFontSize(KotakPdfStyles.FONT_BODY + 2.5f)
                    .setFontColor(KotakPdfStyles.TEXT_DARK)
                    .setMargin(0)
                    .setMarginBottom(KotakPdfStyles.CONTINUATION_LINE_GAP));
        }

        header.add(summaryAccountLine("Account No.", accountNumber)
                .setMarginBottom(KotakPdfStyles.CONTINUATION_LINE_GAP));
        header.add(summaryAccountLine("Account Statement", periodRange).setMarginBottom(0));
        doc.add(withSummaryInset(header));
    }

    private Paragraph summaryAccountLine(String label, String value) {
        return new Paragraph()
                .add(new Text(label + " ")
                        .setFont(KotakPdfStyles.regular())
                        .setFontSize(KotakPdfStyles.FONT_BODY)
                        .setFontColor(KotakPdfStyles.TEXT_MUTED))
                .add(new Text(value)
                        .setFont(KotakPdfStyles.regular())
                        .setFontSize(KotakPdfStyles.FONT_BODY)
                        .setFontColor(KotakPdfStyles.TEXT_DARK))
                .setMargin(0);
    }

    private void addAdKotakBanner(Document doc, KotakPdfImageCache imageCache) {
        Image banner = loadAdKotakBanner(imageCache);
        if (banner == null) {
            return;
        }

        Table bannerTable = new Table(1)
                .useAllAvailableWidth()
                .setMarginTop(KotakPdfStyles.AD_KOTAK_BANNER_GAP)
                .setMarginBottom(KotakPdfStyles.AD_KOTAK_BANNER_GAP);
        bannerTable.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(0)
                .setTextAlignment(TextAlignment.LEFT)
                .add(banner));
        doc.add(withSummaryInset(bannerTable));
    }

    private Image loadAdKotakBanner(KotakPdfImageCache imageCache) {
        try (InputStream in = new ClassPathResource("pdf/kotak/AdKotak.png").getInputStream()) {
            byte[] trimmed = KotakAdBannerImage.loadTrimmedPng(in);
            byte[] optimized = KotakPdfImageOptimizer.toJpeg(
                    KotakPdfImageOptimizer.resizeToMaxWidth(trimmed, 700), 0.92f);
            Image img = new Image(ImageDataFactory.create(optimized));
            img.setWidth(summaryContentWidth());
            img.setAutoScaleHeight(true);
            img.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.LEFT);
            return img;
        } catch (Exception e) {
            log.warn("Kotak AdKotak banner not found", e);
            return null;
        }
    }

    private float summaryContentWidth() {
        return PageSize.A4.getWidth()
                - KotakPdfStyles.MARGIN_LEFT
                - KotakPdfStyles.MARGIN_RIGHT
                - (KotakPdfStyles.SUMMARY_INSET_H * 2f);
    }

    private Image loadImage(KotakPdfImageCache imageCache, String classpath, float height) {
        try {
            return imageCache.scaledImage(classpath, height);
        } catch (Exception e) {
            log.warn("Kotak image not found: {}", classpath);
            return null;
        }
    }

    private String normalizeCustomerName(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace('\u00A0', ' ').trim().replaceAll("\\s+", " ");
    }

    private String formatPeriodRange(StatementPeriod period) {
        if (period == null) return "";
        return formatKotakDate(period.getFromDate()) + " - " + formatKotakDate(period.getToDate());
    }

    private String formatKotakDate(LocalDate date) {
        return date == null ? "" : date.format(KOTAK_DATE);
    }

    private String formatTxnDate(LocalDate date) {
        return date == null ? "" : date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH));
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatDr(BigDecimal debit) {
        if (debit == null || debit.compareTo(BigDecimal.ZERO) == 0) {
            return "";
        }
        return formatAmount(debit.abs());
    }

    private String formatCr(BigDecimal credit) {
        if (credit == null || credit.compareTo(BigDecimal.ZERO) == 0) {
            return "";
        }
        return formatAmount(credit.abs());
    }

    private String deriveCrn(AccountDetails a) {
        if (a == null || a.getAccountNumber() == null || a.getAccountNumber().length() < 3) {
            return "xxxxxx000";
        }
        return "xxxxxx" + a.getAccountNumber().substring(a.getAccountNumber().length() - 3);
    }

    private String deriveMicr(String ifsc) {
        if (ifsc == null || ifsc.length() < 6) return "000000000";
        return ifsc.substring(ifsc.length() - 6) + "507";
    }

    private String buildBranchAddress(Statement statement) {
        CustomerDetails c = statement.getCustomerDetails();
        AccountDetails a = statement.getAccountDetails();
        String city = c != null && c.getCity() != null ? c.getCity() : "City";
        String branch = a != null && a.getBranchName() != null ? a.getBranchName() : "Branch";
        String state = c != null && c.getState() != null ? c.getState() : "State";
        String pin = c != null && c.getPincode() != null ? c.getPincode() : "462001";
        return "Kotak Mahindra Bank Ltd,Ground Floor, Mezzanine Floor,Hotel Blue Star, 62 Hamedia "
                + "Road,Near Sangam Cinema," + city + ", " + city + "-" + pin + ", " + state
                + ", India, 9522262613";
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
