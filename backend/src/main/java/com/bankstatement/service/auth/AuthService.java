package com.bankstatement.service.auth;

import com.bankstatement.config.AppProperties;
import com.bankstatement.dto.*;
import com.bankstatement.entity.OtpPurpose;
import com.bankstatement.entity.OtpToken;
import com.bankstatement.entity.User;
import com.bankstatement.exception.ApiException;
import com.bankstatement.repository.OtpTokenRepository;
import com.bankstatement.repository.UserRepository;
import com.bankstatement.security.JwtService;
import com.bankstatement.security.UserPrincipal;
import com.bankstatement.service.admin.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpSender otpSender;
    private final AppProperties appProperties;
    private final ActivityLogService activityLogService;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpChallengeResponse loginWithEmail(EmailLoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .filter(User::isActive)
                .orElseThrow(() -> new ApiException("Invalid credentials", HttpStatus.UNAUTHORIZED.value()));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException("Invalid credentials", HttpStatus.UNAUTHORIZED.value());
        }

        return createOtpChallenge(user, OtpPurpose.LOGIN);
    }

    public OtpChallengeResponse loginWithMobile(MobileLoginRequest request) {
        User user = userRepository.findByMobile(request.mobile())
                .filter(User::isActive)
                .orElseThrow(() -> new ApiException("Invalid credentials", HttpStatus.UNAUTHORIZED.value()));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException("Invalid credentials", HttpStatus.UNAUTHORIZED.value());
        }

        return createOtpChallenge(user, OtpPurpose.LOGIN);
    }

    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        OtpToken token = otpTokenRepository
                .findByChallengeTokenAndPurposeAndUsedFalse(request.challengeToken(), OtpPurpose.LOGIN)
                .orElseThrow(() -> new ApiException("Invalid or expired OTP session", HttpStatus.BAD_REQUEST.value()));

        validateOtp(token, request.otp());

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND.value()));

        token.setUsed(true);
        otpTokenRepository.save(token);

        UserPrincipal principal = new UserPrincipal(user);
        String jwt = jwtService.generateToken(principal);

        activityLogService.log(user.getId(), user.getName(), "LOGIN", "User logged in successfully", "USER", user.getId());

        return new AuthResponse(jwt, user.getId(), user.getName(), user.getEmail(), user.getMobile(), user.getRole());
    }

    public OtpChallengeResponse forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.email())
                .filter(User::isActive)
                .orElseThrow(() -> new ApiException("If the account exists, an OTP has been sent", HttpStatus.OK.value()));

        return createOtpChallenge(user, OtpPurpose.PASSWORD_RESET);
    }

    public void resetPassword(ResetPasswordRequest request) {
        OtpToken token = otpTokenRepository
                .findByChallengeTokenAndPurposeAndUsedFalse(request.challengeToken(), OtpPurpose.PASSWORD_RESET)
                .orElseThrow(() -> new ApiException("Invalid or expired OTP session", HttpStatus.BAD_REQUEST.value()));

        validateOtp(token, request.otp());

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND.value()));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        token.setUsed(true);
        otpTokenRepository.save(token);

        activityLogService.log(user.getId(), user.getName(), "PASSWORD_RESET", "Password reset completed", "USER", user.getId());
    }

    private OtpChallengeResponse createOtpChallenge(User user, OtpPurpose purpose) {
        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
        String challengeToken = UUID.randomUUID().toString();

        OtpToken otpToken = OtpToken.builder()
                .userId(user.getId())
                .otpHash(passwordEncoder.encode(otp))
                .purpose(purpose)
                .challengeToken(challengeToken)
                .expiresAt(Instant.now().plusSeconds(appProperties.getOtp().getExpiryMinutes() * 60L))
                .used(false)
                .build();

        otpTokenRepository.save(otpToken);

        String recipient = user.getEmail() != null ? user.getEmail() : user.getMobile();
        otpSender.send(recipient, otp, purpose.name());

        return new OtpChallengeResponse(challengeToken, "OTP sent successfully. Check console logs in dev mode.");
    }

    private void validateOtp(OtpToken token, String otp) {
        if (token.isUsed()) {
            throw new ApiException("OTP already used", HttpStatus.BAD_REQUEST.value());
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException("OTP expired", HttpStatus.BAD_REQUEST.value());
        }
        if (!passwordEncoder.matches(otp, token.getOtpHash())) {
            throw new ApiException("Invalid OTP", HttpStatus.BAD_REQUEST.value());
        }
    }
}
