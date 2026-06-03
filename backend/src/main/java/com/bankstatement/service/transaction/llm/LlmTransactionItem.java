package com.bankstatement.service.transaction.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LlmTransactionItem {

    private String date;

    /** credit or debit */
    private String type;

    private BigDecimal amount;

    private String description;

    private String channel;

    @JsonProperty("reference")
    private String reference;
}
