package com.bankstatement.service.pdf;

import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;

/** Paints the top banner background edge-to-edge on page 1 (no side white strips). */
class SbiBannerBackgroundHandler implements IEventHandler {

    @Override
    public void handleEvent(Event event) {
        PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
        if (!PdfDocumentEvent.START_PAGE.equals(docEvent.getType())) {
            return;
        }

        PdfDocument pdf = docEvent.getDocument();
        PdfPage page = docEvent.getPage();
        if (pdf.getPageNumber(page) != 1) {
            return;
        }

        Rectangle pageSize = page.getPageSize();
        float bannerY = pageSize.getHeight() - SbiPdfStyles.BANNER_HEIGHT;

        PdfCanvas canvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdf);
        canvas.saveState();
        canvas.setFillColor(SbiPdfStyles.BANNER_BG);
        canvas.rectangle(0, bannerY, pageSize.getWidth(), SbiPdfStyles.BANNER_HEIGHT);
        canvas.fill();
        canvas.restoreState();
    }
}
