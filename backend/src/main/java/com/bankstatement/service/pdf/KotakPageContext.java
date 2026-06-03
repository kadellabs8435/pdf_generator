package com.bankstatement.service.pdf;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
class KotakPageContext {
    private final String customerName;
    private final String accountNumber;
    private final String periodRange;
    private final String generatedOn;
}
