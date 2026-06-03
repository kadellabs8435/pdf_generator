package com.bankstatement.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDetails {

    private String customerName;
    /** Bank-assigned customer ID (required for BOI statements). */
    private String customerId;
    private LocalDate dateOfBirth;
    private String gender;
    private String email;
    private String address;
    private String city;
    private String state;
    private String pincode;
}
