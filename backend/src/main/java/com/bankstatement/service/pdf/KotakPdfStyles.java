package com.bankstatement.service.pdf;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;

final class KotakPdfStyles {

    static final DeviceRgb KOTAK_RED = new DeviceRgb(237, 28, 36);
    static final DeviceRgb HEADER_GRAY = new DeviceRgb(142, 142, 142);
    static final DeviceRgb TEXT_DARK = new DeviceRgb(17, 17, 17);
    static final DeviceRgb TEXT_MUTED = new DeviceRgb(150, 150, 150);
    static final DeviceRgb BORDER = new DeviceRgb(189, 189, 189);
    static final DeviceRgb WHITE = new DeviceRgb(255, 255, 255);
    static final DeviceRgb BOX_BG = new DeviceRgb(245, 245, 245);
    static final DeviceRgb ICON_BLUE = new DeviceRgb(55, 71, 103);
    static final DeviceRgb LINK_BLUE = new DeviceRgb(0, 102, 204);
    static final DeviceRgb INFO_BOX_BORDER = new DeviceRgb(168, 168, 168);
    static final DeviceRgb INFO_TEXT = new DeviceRgb(34, 34, 34);
    static final DeviceRgb INFO_BULLET = new DeviceRgb(184, 184, 184);

    // Layout tuning to match bank reference export.
    static final float MARGIN_TOP = 10f;
    static final float LOGO_TOP_PADDING = 0f;
    static final float LOGO_ROW_MARGIN_BOTTOM = 10f;
    static final float LOGO_LEFT_HEIGHT = 40f;
    static final float LOGO_RIGHT_HEIGHT = 28f;

    static final float MARGIN_LEFT = 38f;
    static final float MARGIN_RIGHT = 38f;
    static final float MARGIN_BOTTOM = 34f;
    static final float FOOTER_Y = 20f;

    static final float FONT_TITLE = 22f;
    static final float FONT_DATE_RANGE = 9f;
    static final float FONT_BODY = 8f;
    static final float FONT_TABLE = 7f;
    static final float FONT_TABLE_HEADER = 8.5f;

    static final float TITLE_TO_DATE_GAP = 1.5f;
    static final float CUSTOMER_COLUMN_GAP = 54f;
    static final float CUSTOMER_ROW_GAP = 6f;
    static final float LABEL_VALUE_GAP = 14f;

    static final float RED_BAR_HEIGHT = 24f;
    static final float TXN_HEADER_HEIGHT = 22f;
    static final float TXN_ROW_MIN_HEIGHT = 15f;
    static final float TXN_CELL_PAD_H = 4f;
    static final float TXN_CELL_PAD_V = 1f;
    static final float TXN_ROW_LEADING = 1.12f;

    static final float CONTINUATION_TOP_PAD = 28f;
    static final float CONTINUATION_LINE_HEIGHT = 10f;
    static final float CONTINUATION_LINE_GAP = 8f;
    static final float CONTINUATION_BEFORE_RED_GAP = 14f;
    static final float TXN_SECTION_TOP_GAP = 14f;
    /** Top margin reserved on transaction continuation pages for the customer header block. */
    static final float CONTINUATION_RESERVED_TOP =
            CONTINUATION_TOP_PAD
                    + (CONTINUATION_LINE_HEIGHT * 3f)
                    + (CONTINUATION_LINE_GAP * 2f)
                    + CONTINUATION_BEFORE_RED_GAP;

    static final float SUMMARY_INSET_H = 12f;
    static final float SUMMARY_PAGE_HEADER_BOTTOM_GAP = 16f;
    static final float QR_CODE_SIZE = 46f;
    static final float AD_KOTAK_BANNER_GAP = 78f;
    static final float INFO_LINE_HEIGHT = 1.35f;
    static final float INFO_BOX_RADIUS = 18f;
    static final float INFO_BOX_TOP_GAP = 16f;
    static final float INFO_BOX_INSET_H = 6f;
    static final float INFO_BOX_PAD_TOP = 22f;
    static final float INFO_BOX_PAD_BOTTOM = 22f;
    static final float INFO_BOX_PAD_LEFT = 24f;
    static final float INFO_BOX_PAD_RIGHT = 24f;
    static final float INFO_BULLET_SIZE = 7f;
    static final float INFO_BULLET_TEXT_GAP = 12f;
    static final float INFO_ITEM_GAP = 14f;

    static final float[] TXN_COL_WIDTHS = {34, 92, 252, 118, 95, 95, 95};

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
