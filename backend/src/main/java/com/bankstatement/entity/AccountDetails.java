package com.bankstatement.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDetails {

    private String accountNumber;
    private String accountType;
    private String branchName;
    private String ifscCode;
}
