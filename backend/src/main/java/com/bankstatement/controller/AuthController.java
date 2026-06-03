package com.bankstatement.controller;

import com.bankstatement.dto.*;
import com.bankstatement.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login/email")
    public OtpChallengeResponse loginEmail(@Valid @RequestBody EmailLoginRequest request) {
        return authService.loginWithEmail(request);
    }

    @PostMapping("/login/mobile")
    public OtpChallengeResponse loginMobile(@Valid @RequestBody MobileLoginRequest request) {
        return authService.loginWithMobile(request);
    }

    @PostMapping("/verify-otp")
    public AuthResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return authService.verifyOtp(request);
    }

    @PostMapping("/forgot-password")
    public OtpChallengeResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
    }
}
