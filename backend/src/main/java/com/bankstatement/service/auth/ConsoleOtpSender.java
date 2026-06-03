package com.bankstatement.service.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("dev")
public class ConsoleOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleOtpSender.class);

    @Override
    public void send(String recipient, String otp, String purpose) {
        log.info("=== OTP for {} ({}) === OTP: {} ===", recipient, purpose, otp);
    }
}
