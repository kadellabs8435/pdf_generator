package com.bankstatement.service.pdf;

import lombok.Getter;
import lombok.Setter;

/** Tracks SBI PDF page layout (e.g. where summary starts, to skip footer). */
@Getter
@Setter
class SbiPageContext {

    /** First page number (1-based) that contains statement summary; no footer from here on. */
    private int summaryStartPage = Integer.MAX_VALUE;

    boolean shouldDrawFooter(int pageNumber) {
        return pageNumber < summaryStartPage;
    }
}
