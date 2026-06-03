package com.bankstatement.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionSettings {

  private boolean salary;
  /** Employer name shown in salary NEFT remarks. */
  private String salaryCompanyName;
  /** Monthly net salary credited on {@link #salaryDayOfMonth}. */
  private BigDecimal salaryAmount;
  /** Day of month (1–28) for recurring salary NEFT credit. */
  private Integer salaryDayOfMonth;
  private boolean upi;
  private boolean atm;
  private boolean emi;
  private boolean interest;
  private int minTransactions;
  private int maxTransactions;
}
