package com.bankstatement.service.auth;

public interface OtpSender {
    void send(String recipient, String otp, String purpose);
}
