package com.bankstatement.service.pdf;

import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;

class KotakPageEventHandler implements IEventHandler {

    private final KotakPageContext context;
    private Document document;
    private boolean continuationHeaderEnabled = true;

    KotakPageEventHandler(KotakPageContext context) {
        this.context = context;
    }

    void bindDocument(Document document) {
        this.document = document;
    }

    void disableContinuationHeader() {
        this.continuationHeaderEnabled = false;
    }

    @Override
    public void handleEvent(Event event) {
        PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
        if (!PdfDocumentEvent.START_PAGE.equals(docEvent.getType()) || document == null) {
            return;
        }

        PdfDocument pdf = docEvent.getDocument();
        int pageNumber = pdf.getPageNumber(docEvent.getPage());
        if (pageNumber == 1) {
            document.setTopMargin(KotakPdfStyles.MARGIN_TOP);
            return;
        }

        if (continuationHeaderEnabled) {
            document.setTopMargin(KotakPdfStyles.CONTINUATION_RESERVED_TOP);
            drawContinuationHeader(docEvent.getPage(), PageSize.A4, pdf);
        } else {
            document.setTopMargin(KotakPdfStyles.MARGIN_TOP);
        }
    }

    void applyFooters(PdfDocument pdf) {
        int total = pdf.getNumberOfPages();
        Rectangle pageSize = PageSize.A4;
        for (int i = 1; i <= total; i++) {
            drawFooter(pdf.getPage(i), pageSize, pdf, i, total);
        }
    }

    private void drawContinuationHeader(PdfPage page, Rectangle pageSize, PdfDocument pdf) {
        PdfCanvas canvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdf);
        Canvas layoutCanvas = new Canvas(canvas, pageSize);
        float left = KotakPdfStyles.MARGIN_LEFT;
        float y = pageSize.getTop() - KotakPdfStyles.CONTINUATION_TOP_PAD;

        layoutCanvas.showTextAligned(
                new Paragraph(context.getCustomerName())
                        .setFont(KotakPdfStyles.bold())
                        .setFontSize(KotakPdfStyles.FONT_BODY + 2.5f)
                        .setFontColor(KotakPdfStyles.TEXT_DARK)
                        .setMargin(0),
                left, y, TextAlignment.LEFT);

        y -= KotakPdfStyles.CONTINUATION_LINE_HEIGHT + KotakPdfStyles.CONTINUATION_LINE_GAP;
        layoutCanvas.showTextAligned(accountLine("Account No.", context.getAccountNumber()), left, y, TextAlignment.LEFT);

        y -= KotakPdfStyles.CONTINUATION_LINE_HEIGHT + KotakPdfStyles.CONTINUATION_LINE_GAP;
        layoutCanvas.showTextAligned(accountLine("Account Statement", context.getPeriodRange()), left, y, TextAlignment.LEFT);

        layoutCanvas.close();
    }

    private Paragraph accountLine(String label, String value) {
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

    private void drawFooter(PdfPage page, Rectangle pageSize, PdfDocument pdf, int pageNumber, int totalPages) {
        PdfCanvas canvas = new PdfCanvas(page.newContentStreamAfter(), page.getResources(), pdf);
        Canvas layoutCanvas = new Canvas(canvas, pageSize);

        layoutCanvas.showTextAligned(
                new Paragraph("Statement Generated on " + context.getGeneratedOn())
                        .setFont(KotakPdfStyles.regular())
                        .setFontSize(7)
                        .setFontColor(KotakPdfStyles.TEXT_MUTED)
                        .setMargin(0),
                KotakPdfStyles.MARGIN_LEFT, KotakPdfStyles.FOOTER_Y, TextAlignment.LEFT);

        layoutCanvas.showTextAligned(
                new Paragraph("Page " + pageNumber + " of " + totalPages)
                        .setFont(KotakPdfStyles.regular())
                        .setFontSize(7)
                        .setFontColor(KotakPdfStyles.TEXT_MUTED)
                        .setMargin(0),
                pageSize.getWidth() - KotakPdfStyles.MARGIN_RIGHT, KotakPdfStyles.FOOTER_Y, TextAlignment.RIGHT);

        layoutCanvas.close();
    }
}
