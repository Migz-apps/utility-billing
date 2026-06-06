package com.miguel.app.system.service.impl;

import com.miguel.app.system.dto.response.OtpDispatchResponse;
import com.miguel.app.system.entity.EmailVerificationOtp;
import com.miguel.app.system.entity.User;
import com.miguel.app.system.enums.Role;
import com.miguel.app.system.enums.UserStatus;
import com.miguel.app.system.exception.BadRequestException;
import com.miguel.app.system.exception.ResourceNotFoundException;
import com.miguel.app.system.repository.EmailVerificationOtpRepository;
import com.miguel.app.system.repository.UserRepository;
import com.miguel.app.system.service.interfaces.EmailVerificationService;
import com.miguel.app.system.service.interfaces.MailService;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final MailService mailService;

    @Value("${app.auth.otp.expiry-minutes:10}")
    private long otpExpiryMinutes;

    @Override
    @Transactional
    public OtpDispatchResponse issueVerificationOtp(User user) {
        invalidateExistingOtps(user);

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(otpExpiryMinutes);
        String code = generateOtpCode();

        EmailVerificationOtp otp = new EmailVerificationOtp();
        otp.setUser(user);
        otp.setCode(code);
        otp.setExpiresAt(expiresAt);
        otp.setUsed(false);
        otpRepository.save(otp);

        mailService.sendVerificationOtp(user.getEmail(), user.getFullName(), code, expiresAt);
        return new OtpDispatchResponse(user.getEmail(), expiresAt);
    }

    @Override
    @Transactional
    public OtpDispatchResponse resendVerificationOtp(String email) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("No account found for this email"));

        if (user.getRole() != Role.ROLE_CUSTOMER) {
            throw new BadRequestException("Only customer accounts can use email verification");
        }

        if (user.isEmailVerified()) {
            throw new BadRequestException("Email is already verified");
        }

        return issueVerificationOtp(user);
    }

    @Override
    @Transactional
    public void verifyEmailOtp(String email, String otpCode) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("No account found for this email"));

        if (user.getRole() != Role.ROLE_CUSTOMER) {
            throw new BadRequestException("Only customer accounts can use email verification");
        }

        if (user.isEmailVerified()) {
            throw new BadRequestException("Email is already verified");
        }

        EmailVerificationOtp otp = otpRepository.findFirstByUserAndUsedFalseOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new BadRequestException("No active verification code found for this email"));

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            otp.setUsed(true);
            otp.setUsedAt(LocalDateTime.now());
            otpRepository.save(otp);
            throw new BadRequestException("Verification code has expired");
        }

        if (!otp.getCode().equals(otpCode)) {
            throw new BadRequestException("Invalid verification code");
        }

        otp.setUsed(true);
        otp.setUsedAt(LocalDateTime.now());
        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);

        otpRepository.save(otp);
        userRepository.save(user);
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 */30 * * * *")
    public void cleanupExpiredOtps() {
        otpRepository.deleteByExpiresAtBefore(LocalDateTime.now().minusDays(1));
    }

    private void invalidateExistingOtps(User user) {
        for (EmailVerificationOtp existingOtp : otpRepository.findAllByUserAndUsedFalse(user)) {
            existingOtp.setUsed(true);
            existingOtp.setUsedAt(LocalDateTime.now());
        }
    }

    private String generateOtpCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
