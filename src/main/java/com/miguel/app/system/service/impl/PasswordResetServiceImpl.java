package com.miguel.app.system.service.impl;

import com.miguel.app.system.dto.response.OtpDispatchResponse;
import com.miguel.app.system.entity.PasswordResetOtp;
import com.miguel.app.system.entity.User;
import com.miguel.app.system.enums.UserStatus;
import com.miguel.app.system.exception.BadRequestException;
import com.miguel.app.system.exception.ResourceNotFoundException;
import com.miguel.app.system.repository.PasswordResetOtpRepository;
import com.miguel.app.system.repository.UserRepository;
import com.miguel.app.system.service.interfaces.MailService;
import com.miguel.app.system.service.interfaces.PasswordResetService;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final PasswordResetOtpRepository resetOtpRepository;
    private final UserRepository userRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.auth.otp.expiry-minutes:10}")
    private long otpExpiryMinutes;

    @Override
    @Transactional
    public OtpDispatchResponse sendResetOtp(String email) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("No account found for this email"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("Account is inactive. Please contact support.");
        }

        invalidateExistingOtps(user);

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(otpExpiryMinutes);
        String code = generateOtpCode();

        PasswordResetOtp otp = new PasswordResetOtp();
        otp.setUser(user);
        otp.setCode(code);
        otp.setExpiresAt(expiresAt);
        otp.setUsed(false);
        resetOtpRepository.save(otp);

        mailService.sendPasswordResetOtp(user.getEmail(), user.getFullName(), code, expiresAt);
        return new OtpDispatchResponse(user.getEmail(), expiresAt);
    }

    @Override
    @Transactional
    public void resetPassword(String email, String otpCode, String newPassword) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("No account found for this email"));

        PasswordResetOtp otp = resetOtpRepository.findFirstByUserAndUsedFalseOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new BadRequestException("No active reset code found. Please request a new one."));

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            otp.setUsed(true);
            otp.setUsedAt(LocalDateTime.now());
            resetOtpRepository.save(otp);
            throw new BadRequestException("Reset code has expired. Please request a new one.");
        }

        if (!otp.getCode().equals(otpCode)) {
            throw new BadRequestException("Invalid reset code.");
        }

        otp.setUsed(true);
        otp.setUsedAt(LocalDateTime.now());
        user.setPassword(passwordEncoder.encode(newPassword));

        resetOtpRepository.save(otp);
        userRepository.save(user);
    }

    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    public void cleanupExpiredOtps() {
        resetOtpRepository.deleteByExpiresAtBefore(LocalDateTime.now().minusDays(1));
    }

    private void invalidateExistingOtps(User user) {
        for (PasswordResetOtp existing : resetOtpRepository.findAllByUserAndUsedFalse(user)) {
            existing.setUsed(true);
            existing.setUsedAt(LocalDateTime.now());
        }
    }

    private String generateOtpCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
