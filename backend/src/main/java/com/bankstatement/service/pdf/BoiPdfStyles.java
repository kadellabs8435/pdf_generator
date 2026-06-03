package com.bankstatement.service.pdf;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.geom.PageSize;

/** Typography and layout constants for Bank of India detailed statements. */
final class BoiPdfStyles {

    static final float MARGIN_LEFT = 36f;
    static final float MARGIN_RIGHT = 36f;
    static final float MARGIN_TOP = 28f;
    static final float MARGIN_BOTTOM = 40f;

    static final float FONT_TITLE = 14f;
    static final float FONT_BODY = 7.75f;
    static final float FONT_LABEL = 8f;
    static final float FONT_TABLE = 8f;
    static final float LINE_HEIGHT_TABLE = 1.18f;
    static final float FONT_FILTER = 8f;
    static final float FONT_DATE = 8f;

    static final float LOGO_HEIGHT = 26f;
    /** Negative margin pulls logo right to align star tip with date/box right edge. */
    static final float LOGO_SHIFT_RIGHT = -50f;

    static final float LINE_HEIGHT = 1.12f;
    static final float LINE_HEIGHT_DETAIL = 1.28f;

    static final DeviceRgb BLACK = new DeviceRgb(0, 0, 0);
    static final DeviceRgb TEXT_DARK = new DeviceRgb(35, 35, 35);
    static final DeviceRgb BORDER = new DeviceRgb(0, 0, 0);

    static final float DETAIL_BORDER_WIDTH = 0.25f;
    static final float DETAIL_PAD_H = 7f;
    static final float DETAIL_PAD_V = 6f;
    /** Equal vertical gap between rows inside the customer detail box. */
    static final float DETAIL_LINE_GAP = 3.5f;

    /** Fixed 4-column customer grid (pt): left label | left value | right label | right value. */
    static final float CUSTOMER_GRID_COL1 = 108f;
    static final float CUSTOMER_GRID_COL2 = 132f;
    static final float CUSTOMER_GRID_COL3 = 108f;
    static final float CUSTOMER_GRID_COL4 = customerDetailInnerWidth()
            - CUSTOMER_GRID_COL1 - CUSTOMER_GRID_COL2 - CUSTOMER_GRID_COL3;
    static final float[] CUSTOMER_GRID_COL_WIDTHS = {
            CUSTOMER_GRID_COL1, CUSTOMER_GRID_COL2, CUSTOMER_GRID_COL3, CUSTOMER_GRID_COL4
    };

    static float customerDetailInnerWidth() {
        return PageSize.A4.getWidth() - MARGIN_LEFT - MARGIN_RIGHT - (2 * DETAIL_PAD_H);
    }

    /** Filter: label | from | to */
    static final float[] FILTER_COL_WIDTHS = {100f, 198f, 198f};
    static final float FILTER_ROW_GAP = 4.5f;
    static final float FILTER_SECTION_MARGIN_TOP = 10f;

    static final float TITLE_MARGIN_TOP = 8f;
    static final float TITLE_MARGIN_BOTTOM = 10f;
    static final float TITLE_PADDING_BOTTOM = 4f;
    static final float DATE_MARGIN_BOTTOM = 12f;
    static final float BOX_MARGIN_BOTTOM = 2f;
    static final float TXN_TYPE_MARGIN_TOP = 7f;
    static final float TXN_TYPE_MARGIN_BOTTOM = 6f;
    static final float TABLE_MARGIN_TOP = 4f;

    /**
     * Sr No ~6%, Date ~11%, Remarks ~43%, Debit/Credit ~13% each, Balance ~13% (~523pt).
     * Remarks width tuned so long UPI narrations wrap to two lines like the reference statement.
     */
    static final float[] TXN_COL_WIDTHS = {36f, 60f, 218f, 70f, 70f, 69f};

    /** Transaction table grid — thicker lines matching BOI statement export. */
    static final float TXN_BORDER_WIDTH = 0.85f;
    static final float TXN_HEADER_PAD_V = 5f;
    static final float TXN_HEADER_PAD_H = 4f;
    static final float TXN_HEADER_MIN_HEIGHT = 20f;
    static final float TXN_BODY_PAD_V = 5f;
    static final float TXN_BODY_PAD_H = 4f;
    static final float TXN_BODY_MIN_HEIGHT = 18f;
    static final float TXN_AMOUNT_PAD_RIGHT = 4f;
    static final float TXN_BALANCE_PAD_RIGHT = 4f;

    /** Footer note below transaction table — aligned with table left edge. */
    static final float NOTE_MARGIN_TOP = 6f;
    static final float NOTE_HEADING_MARGIN_BOTTOM = 2f;
    static final float FONT_NOTE_HEADING = 8f;
    static final float FONT_NOTE_BODY = 7.5f;
    static final float LINE_HEIGHT_NOTE = 1.12f;

    static final String NOTE_HEADING = "NOTE:";
    static final String NOTE_BODY =
            "Any discrepancy in the account statement should be notified to the bank within period of "
                    + "30 days of generation of statement. It will be treated that the entries/contents of this "
                    + "statement are checked and found correct by you, if no such complaint is made within the "
                    + "period stated above. Please do not share your ATM, Card details, PIN, OTP and Passwords "
                    + "with anyone else. Bank never asks for such details.";

    private BoiPdfStyles() {}

    static PdfFont regular() {
        return BankPdfFonts.regular();
    }

    static PdfFont bold() {
        return BankPdfFonts.bold();
    }

    static void clearCache() {
        BankPdfFonts.clearCache();
    }
}
