package com.bankstatement.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    private LocalDate date;
    private String narration;
    private String reference;
    private String type;
    private BigDecimal debit;
    private BigDecimal credit;
    private BigDecimal balance;
    /** Display-only amount when balance processing cleared the credit column. */
    private BigDecimal renderCredit;
    /** Display-only amount when balance processing cleared the debit column. */
    private BigDecimal renderDebit;
}
