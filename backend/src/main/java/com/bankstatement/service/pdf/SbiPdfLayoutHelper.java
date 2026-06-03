package com.bankstatement.service.pdf;

import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Tab;
import com.itextpdf.layout.element.TabStop;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TabAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

/** Reusable icon + vertical divider rows matching SBI statement layout. */
final class SbiPdfLayoutHelper {

    private static final float TEXT_INDENT = SbiPdfStyles.ICON_COL + SbiPdfStyles.ICON_DIVIDER_GAP
            + SbiPdfStyles.DIVIDER_COL + SbiPdfStyles.DIVIDER_TEXT_GAP + SbiPdfStyles.TEXT_PAD_AFTER_DIVIDER;

    private SbiPdfLayoutHelper() {}

    static Table iconTextRow(String icon, String text, float fontSize) {
        return iconTextRow(icon, new String[]{text}, fontSize, false, true, false);
    }

    static Table iconTextRowRight(String icon, String text, float fontSize) {
        return iconTextRow(icon, new String[]{text}, fontSize, false, true, true);
    }

    static Table iconLabelValueRows(String icon, String[][] labelValues, float fontSize) {
        return iconTextRow(icon, toLines(labelValues), fontSize, true, true, false);
    }

    static Table iconLabelValueRowsRight(String icon, String[][] labelValues, float fontSize) {
        return iconTextRow(icon, toLines(labelValues), fontSize, true, true, true);
    }

    static Table dividerLabelValueRows(String[][] labelValues, float fontSize) {
        return labelValueRowsOnly(labelValues, fontSize);
    }

    static Table dividerLabelValueRowsWithMargin(String[][] labelValues, float fontSize, float marginTop, float marginBottom) {
        Table block = labelValueRowsOnly(labelValues, fontSize);
        block.setMarginTop(marginTop);
        block.setMarginBottom(marginBottom);
        return block;
    }

    private static String[] toLines(String[][] labelValues) {
        String[] lines = new String[labelValues.length];
        for (int i = 0; i < labelValues.length; i++) {
            lines[i] = labelValues[i][0] + " : " + labelValues[i][1];
        }
        return lines;
    }

    private static Table labelValueRowsOnly(String[][] labelValues, float fontSize) {
        Table block = new Table(1)
                .useAllAvailableWidth()
                .setBorder(Border.NO_BORDER)
                .setMarginBottom(SbiPdfStyles.CUSTOMER_ROW_GAP);
        Cell cell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPaddingLeft(TEXT_INDENT)
                .setPaddingTop(0)
                .setPaddingBottom(0);
        for (String line : toLines(labelValues)) {
            cell.add(buildLabelValueParagraph(line, fontSize));
        }
        block.addCell(cell);
        return block;
    }

    private static Table iconTextRow(String icon, String[] lines, float fontSize, boolean labelStyle,
                                     boolean showDivider, boolean rightColumn) {
        boolean multiLine = lines.length > 1 || (lines.length == 1 && lines[0] != null && lines[0].length() > 55);
        VerticalAlignment iconAlign = multiLine ? VerticalAlignment.MIDDLE : VerticalAlignment.MIDDLE;

        Table row = new Table(UnitValue.createPointArray(new float[]{
                SbiPdfStyles.ICON_COL, SbiPdfStyles.DIVIDER_COL, 400f}))
                .useAllAvailableWidth()
                .setBorder(Border.NO_BORDER)
                .setMarginBottom(SbiPdfStyles.CUSTOMER_ROW_GAP);

        Cell iconCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(0)
                .setPaddingRight(SbiPdfStyles.ICON_DIVIDER_GAP)
                .setVerticalAlignment(iconAlign);
        if (rightColumn) {
            iconCell.setPaddingTop(SbiPdfStyles.RIGHT_ICON_OFFSET);
        }
        if (icon != null && !icon.isEmpty()) {
            iconCell.add(new Paragraph(icon)
                    .setFont(SbiPdfIcons.font())
                    .setFontSize(SbiPdfStyles.FONT_BODY)
                    .setFontColor(SbiPdfStyles.ICON_PURPLE)
                    .setMargin(0)
                    .setMultipliedLeading(1f));
        }

        Cell dividerCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(0)
                .setPaddingRight(SbiPdfStyles.DIVIDER_TEXT_GAP)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        if (rightColumn) {
            dividerCell.setPaddingTop(SbiPdfStyles.RIGHT_ICON_OFFSET);
        }
        if (showDivider && icon != null && !icon.isEmpty()) {
            dividerCell.setBorderLeft(new SolidBorder(SbiPdfStyles.ICON_PURPLE, 1f));
        }

        Cell textCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPaddingLeft(SbiPdfStyles.TEXT_PAD_AFTER_DIVIDER)
                .setPaddingTop(0)
                .setPaddingBottom(0)
                .setVerticalAlignment(VerticalAlignment.TOP);
        for (String line : lines) {
            if (labelStyle) {
                textCell.add(buildLabelValueParagraph(line, fontSize));
            } else {
                textCell.add(new Paragraph(line)
                        .setFont(SbiPdfStyles.regular())
                        .setFontSize(fontSize)
                        .setFontColor(SbiPdfStyles.TEXT)
                        .setMargin(0)
                        .setMultipliedLeading(SbiPdfStyles.TXN_LINE_HEIGHT));
            }
        }

        row.addCell(iconCell);
        row.addCell(dividerCell);
        row.addCell(textCell);
        return row;
    }

    private static Paragraph buildLabelValueParagraph(String line, float fontSize) {
        if (!line.contains(" : ")) {
            return new Paragraph(line)
                    .setFont(SbiPdfStyles.regular())
                    .setFontSize(fontSize)
                    .setFontColor(SbiPdfStyles.TEXT)
                    .setMargin(0)
                    .setMultipliedLeading(SbiPdfStyles.TXN_LINE_HEIGHT);
        }
        int idx = line.indexOf(" : ");
        String labelPart = line.substring(0, idx + 3);
        String valuePart = line.substring(idx + 3);
        return new Paragraph()
                .addTabStops(new TabStop(SbiPdfStyles.LABEL_VALUE_TAB + SbiPdfStyles.LABEL_VALUE_GAP, TabAlignment.LEFT))
                .add(new Text(labelPart).setFont(SbiPdfStyles.bold()).setFontSize(fontSize))
                .add(new Tab())
                .add(new Text(valuePart).setFont(SbiPdfStyles.regular()).setFontSize(fontSize))
                .setMargin(0)
                .setMarginBottom(0)
                .setMultipliedLeading(SbiPdfStyles.TXN_LINE_HEIGHT);
    }
}
