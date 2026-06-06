package com.miguel.app.system.service.impl;

import com.miguel.app.system.exception.BusinessRuleException;
import com.miguel.app.system.service.interfaces.MailService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private static final DateTimeFormatter OTP_EXPIRY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Override
    public void sendVerificationOtp(String recipientEmail, String recipientName, String otpCode, LocalDateTime expiresAt) {
        if (!mailEnabled) {
            log.warn("Email delivery is disabled. Verification OTP for {} was not sent.", recipientEmail);
            throw new BusinessRuleException("Email verification is currently unavailable. Please contact support.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(recipientEmail);
        message.setSubject("Utility Billing System email verification code");
        message.setText("""
                Hello %s,

                Your Utility Billing System verification code is: %s

                This code expires at %s.

                If you did not create this account, please ignore this email.
                """.formatted(
                recipientName,
                otpCode,
                expiresAt.format(OTP_EXPIRY_FORMATTER)
        ));

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            log.error("Failed to send verification OTP to {}", recipientEmail, ex);
            throw new BusinessRuleException("Unable to send verification email right now. Please try again.");
        }
    }

    @Override
    public void sendPasswordResetOtp(String recipientEmail, String recipientName, String otpCode, LocalDateTime expiresAt) {
        if (!mailEnabled) {
            log.warn("Email delivery is disabled. Password reset OTP for {} was not sent.", recipientEmail);
            throw new BusinessRuleException("Email service is currently unavailable. Please contact support.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(recipientEmail);
        message.setSubject("Utility Billing System password reset code");
        message.setText("""
                Hello %s,

                Your Utility Billing System password reset code is: %s

                This code expires at %s.

                If you did not request a password reset, please ignore this email.
                Your password will not be changed unless you submit this code.
                """.formatted(
                recipientName,
                otpCode,
                expiresAt.format(OTP_EXPIRY_FORMATTER)
        ));

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            log.error("Failed to send password reset OTP to {}", recipientEmail, ex);
            throw new BusinessRuleException("Unable to send password reset email right now. Please try again.");
        }
    }
}
