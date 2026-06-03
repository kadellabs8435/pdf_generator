package com.bankstatement.service.pdf;

import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;

class SbiPageEventHandler implements IEventHandler {

    @SuppressWarnings("unused")
    private final boolean includeWatermark;
    private final SbiPageContext pageContext;

    SbiPageEventHandler(SbiPageContext pageContext, boolean includeWatermark) {
        this.pageContext = pageContext;
        this.includeWatermark = includeWatermark;
    }

    @Override
    public void handleEvent(Event event) {
        PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
        if (!PdfDocumentEvent.END_PAGE.equals(docEvent.getType())) {
            return;
        }

        PdfDocument pdf = docEvent.getDocument();
        PdfPage page = docEvent.getPage();
        Rectangle pageSize = page.getPageSize();
        int pageNumber = pdf.getPageNumber(page);

        if (!pageContext.shouldDrawFooter(pageNumber)) {
            return;
        }

        PdfCanvas pdfCanvas = new PdfCanvas(page.newContentStreamAfter(), page.getResources(), pdf);
        Canvas canvas = new Canvas(pdfCanvas, pageSize);
        canvas.showTextAligned(
                new Paragraph("Page no. " + pageNumber)
                        .setFont(SbiPdfStyles.regular())
                        .setFontSize(SbiPdfStyles.FONT_FOOTER)
                        .setFontColor(SbiPdfStyles.TEXT)
                        .setMargin(0),
                pageSize.getWidth() / 2,
                SbiPdfStyles.FOOTER_PAGE_NO_Y,
                TextAlignment.CENTER);
        canvas.close();
    }
}
