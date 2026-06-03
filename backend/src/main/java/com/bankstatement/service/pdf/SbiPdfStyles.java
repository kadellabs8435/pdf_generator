package com.bankstatement.service.pdf;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;

final class SbiPdfStyles {

    static final Color TABLE_HEADER = new DeviceRgb(91, 91, 167);
    /** Transaction table bottom strip (#4f46b5). */
    static final Color TABLE_FOOTER_STRIP = new DeviceRgb(79, 70, 181);
    static final Color PRIMARY = TABLE_HEADER;
    static final Color BANNER_BG = new DeviceRgb(75, 79, 161);
    static final Color ICON_PURPLE = new DeviceRgb(108, 99, 210);
    static final Color BANK_TITLE = new DeviceRgb(88, 101, 186);
    static final Color TEXT = new DeviceRgb(17, 17, 17);
    static final Color BORDER = new DeviceRgb(189, 189, 189);
    static final Color TITLE_LINE = new DeviceRgb(180, 195, 220);
    static final Color WHITE = new DeviceRgb(255, 255, 255);

    static final float FONT_TITLE = 13f;
    static final float FONT_BODY = 9f;
    static final float FONT_TABLE = 8.5f;
    static final float FONT_FOOTER = 8f;

    static final float PAGE_WIDTH = 595.28f;

    static final float MARGIN_TOP = 0;
    static final float MARGIN_LEFT = 20;
    static final float MARGIN_RIGHT = 20;
    static final float BANNER_HEIGHT = 60f;

    static final float BANNER_PAD_LEFT = 18f;
    static final float BANNER_PAD_RIGHT = 18f;
    static final float BANNER_PAD_TOP = 6f;
    static final float BANNER_PAD_BOTTOM = 6f;
    /** Fixed width for banner date pill — avoids underestimating text width and wrapping. */
    static final float BANNER_DATE_BOX_WIDTH = 100f;
    /** Compact date pill horizontal/vertical padding (see bannerCenterCell). */
    static final float BANNER_DATE_BOX_PAD_H = 5f;
    static final float BANNER_DATE_BOX_PAD_V = 3f;
    /** Logo width multiplier vs Account Summary label width. */
    static final float BANNER_LOGO_WIDTH_SCALE = 1.25f;
    /** Account Summary aligns with SBI wordmark inside combined logo asset. */
    static final float BANNER_LOGO_TEXT_OFFSET_RATIO = 0.34f;
    static final float BANNER_WELCOME_NAME_SIZE = 10f;

    static final float TITLE_MARGIN_TOP = 12f;
    static final float TITLE_LINE_MARGIN_TOP = 8f;
    static final float TITLE_LINE_MARGIN_BOTTOM = 8f;
    static final float TITLE_TO_CUSTOMER_GAP = 10f;

    static final float ICON_COL = 14f;
    static final float ICON_DIVIDER_GAP = 6f;
    static final float DIVIDER_COL = 1.5f;
    static final float DIVIDER_TEXT_GAP = 8f;
    static final float TEXT_PAD_AFTER_DIVIDER = 6f;
    static final float RIGHT_ICON_OFFSET = 2.5f;
    static final float LABEL_VALUE_TAB = 108f;
    static final float LABEL_VALUE_GAP = 10f;

    static final float CUSTOMER_ROW_GAP = 8f;
    static final float CUSTOMER_SECTION_GAP = 14f;
    static final float CUSTOMER_COLUMN_INNER_GAP = 15f;
    static final float BALANCE_BLOCK_MARGIN_V = 8f;

    static final float PERIOD_MARGIN_TOP = 12f;
    static final float PERIOD_MARGIN_BOTTOM = 0f;
    static final float PERIOD_TO_TABLE_GAP = 12f;

    static final float TABLE_MARGIN_TOP = 12f;
    static final float TABLE_INSET_H = 12f;
    static final float TXN_HEADER_MIN_HEIGHT = 36f;
    static final float TXN_HEADER_PADDING_TOP = 8f;
    static final float TXN_HEADER_PADDING_BOTTOM = 8f;
    static final float TXN_HEADER_REPEAT_TOP_PAD = 10f;
    static final float TXN_ROW_MIN_HEIGHT = 44f;
    static final float TXN_BODY_PAD_V = 6f;
    static final float TXN_LINE_HEIGHT = 1.25f;

    static final float TXN_FOOTER_ROW_HEIGHT = 24f;
    static final float SUMMARY_MARGIN_BOTTOM = 36f;
    static final float FOOTER_PAGE_NO_Y = 32f;

    /** Space below content for page number (footer strip is inside the table). */
    static final float MARGIN_BOTTOM = FOOTER_PAGE_NO_Y + 22f;

    static final float[] TXN_COL_WIDTHS = {54, 54, 154, 54, 77, 77, 85};

    static final float SUMMARY_TOP_GAP = 14f;
    static final float SUMMARY_INSET_H = 12f;
    static final float SUMMARY_BAR_HEIGHT = 23f;
    static final float SUMMARY_HEADER_ROW_HEIGHT = 28f;
    static final float SUMMARY_VALUE_ROW_HEIGHT = 26f;

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
