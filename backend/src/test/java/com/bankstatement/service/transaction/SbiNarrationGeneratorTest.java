package com.bankstatement.service.transaction;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class SbiNarrationGeneratorTest {

    private static final Pattern UPI_DEBIT = Pattern.compile("^TO TRANSFER-UPI/[A-Z ]+$");
    private static final Pattern UPI_CREDIT = Pattern.compile("^BY TRANSFER-UPI/[A-Z ]+$");
    private static final Pattern IMPS_CREDIT = Pattern.compile("^BY TRANSFER-IMPS/[A-Z ]+$");
    private static final Pattern NEFT_CREDIT = Pattern.compile("^BY TRANSFER-NEFT/[A-Z ]+$");
    private static final Pattern SALARY = Pattern.compile("^BY TRANSFER-SALARY/.+$");

    @Test
    void usesRealisticTransferFormats() {
        Random random = new Random(42);
        String party = "RAJESH SHARMA";
        assertTrue(UPI_DEBIT.matcher(SbiNarrationGenerator.upiDebit(random, party).narration()).matches());
        assertTrue(UPI_CREDIT.matcher(SbiNarrationGenerator.upiCredit(random, party).narration()).matches());
        assertTrue(IMPS_CREDIT.matcher(SbiNarrationGenerator.impsCredit(random, party).narration()).matches());
        assertTrue(NEFT_CREDIT.matcher(SbiNarrationGenerator.neftCredit(random, party).narration()).matches());
        assertEquals("ATM CASH WDL", SbiNarrationGenerator.atmWithdrawal(random).narration());
        assertEquals("INTEREST CREDIT", SbiNarrationGenerator.interestCredit(random).narration());
    }

    @Test
    void configuredSalaryUsesCompanyFromForm() {
        Random random = new Random(7);
        BankRemarkGenerator.SalaryRemark salary =
                SbiNarrationGenerator.configuredSalary("Tata Consultancy Services Ltd", random);
        assertTrue(SALARY.matcher(salary.narration()).matches(), salary.narration());
        assertTrue(salary.narration().contains("TATA CONSULTANCY SERVICES LTD"));
        assertNotNull(salary.reference());
        assertTrue(salary.reference().startsWith("NEFTINW-"));
    }
}
