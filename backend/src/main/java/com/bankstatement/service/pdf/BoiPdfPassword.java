package com.bankstatement.service.pdf;

import com.bankstatement.entity.CustomerDetails;
import com.bankstatement.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Locale;

/** BOI statement PDF password: first 4 letters of first name (or full if shorter) + DDMM from DOB. */
final class BoiPdfPassword {

    private BoiPdfPassword() {}

    static String generate(CustomerDetails customer) {
        if (customer == null || customer.getCustomerName() == null || customer.getCustomerName().isBlank()) {
            throw new ApiException("Customer name is required for BOI PDF password", HttpStatus.BAD_REQUEST.value());
        }
        if (customer.getDateOfBirth() == null) {
            throw new ApiException("Date of birth is required for BOI PDF password", HttpStatus.BAD_REQUEST.value());
        }
        return generate(customer.getCustomerName(), customer.getDateOfBirth());
    }

    static String generate(String customerName, LocalDate dateOfBirth) {
        if (customerName == null || customerName.isBlank()) {
            throw new ApiException("Customer name is required for BOI PDF password", HttpStatus.BAD_REQUEST.value());
        }
        if (dateOfBirth == null) {
            throw new ApiException("Date of birth is required for BOI PDF password", HttpStatus.BAD_REQUEST.value());
        }

        String firstWord = customerName.trim().split("\\s+")[0].toUpperCase(Locale.ROOT);
        String namePart = firstWord.length() >= 4 ? firstWord.substring(0, 4) : firstWord;
        String ddmm = String.format(Locale.ROOT, "%02d%02d",
                dateOfBirth.getDayOfMonth(), dateOfBirth.getMonthValue());
        return namePart + ddmm;
    }
}
