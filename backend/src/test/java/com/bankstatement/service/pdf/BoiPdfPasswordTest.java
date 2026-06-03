package com.bankstatement.service.pdf;

import com.bankstatement.entity.CustomerDetails;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BoiPdfPasswordTest {

    @Test
    void pavanSinghExample() {
        assertEquals("PAVA2401", BoiPdfPassword.generate("Pavan Singh", LocalDate.of(1998, 1, 24)));
    }

    @Test
    void vinodKumarExample() {
        assertEquals("VINO0511", BoiPdfPassword.generate("Vinod Kumar", LocalDate.of(1994, 11, 5)));
    }

    @Test
    void amitExample() {
        assertEquals("AMIT0812", BoiPdfPassword.generate("Amit", LocalDate.of(1999, 12, 8)));
    }

    @Test
    void omExample() {
        assertEquals("OM1503", BoiPdfPassword.generate("Om", LocalDate.of(1996, 3, 15)));
    }

    @Test
    void fromCustomerDetails() {
        CustomerDetails c = CustomerDetails.builder()
                .customerName("Pavan Singh")
                .dateOfBirth(LocalDate.of(1998, 1, 24))
                .build();
        assertEquals("PAVA2401", BoiPdfPassword.generate(c));
    }
}
