package com.miguel.app.system;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miguel.app.system.entity.PasswordResetOtp;
import com.miguel.app.system.entity.User;
import com.miguel.app.system.enums.Role;
import com.miguel.app.system.enums.UserStatus;
import com.miguel.app.system.repository.PasswordResetOtpRepository;
import com.miguel.app.system.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.mail.enabled=true")
class PasswordResetFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetOtpRepository resetOtpRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JavaMailSender javaMailSender;

    private static final String EMAIL = "reset.user@example.com";
    private static final String ORIGINAL_PASSWORD = "Original123!";

    @BeforeEach
    void setUp() {
        resetOtpRepository.deleteAll();
        userRepository.findByEmail(EMAIL).ifPresent(u -> {
            resetOtpRepository.deleteAll();
            userRepository.delete(u);
        });

        User user = new User();
        user.setFullName("Reset User");
        user.setEmail(EMAIL);
        user.setPhoneNumber("0781111111");
        user.setPassword(passwordEncoder.encode(ORIGINAL_PASSWORD));
        user.setRole(Role.ROLE_CUSTOMER);
        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }

    // -----------------------------------------------------------------------
    // Happy path: full flow
    // -----------------------------------------------------------------------

    @Test
    void fullResetFlow_loginWithNewPasswordSucceeds() throws Exception {
        // Step 1 — request OTP
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("email", EMAIL))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(EMAIL));

        // Step 2 — grab OTP from DB (no real email in tests)
        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        PasswordResetOtp otp = resetOtpRepository
                .findFirstByUserAndUsedFalseOrderByCreatedAtDesc(user).orElseThrow();

        // Step 3 — reset with correct OTP
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", EMAIL,
                                "otpCode", otp.getCode(),
                                "newPassword", "NewPassword123!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Step 4 — old password no longer works
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", EMAIL,
                                "password", ORIGINAL_PASSWORD
                        ))))
                .andExpect(status().isUnauthorized());

        // Step 5 — new password works
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", EMAIL,
                                "password", "NewPassword123!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    // -----------------------------------------------------------------------
    // OTP is marked used after reset — cannot reuse it
    // -----------------------------------------------------------------------

    @Test
    void usedOtp_cannotBeReusedForSecondReset() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("email", EMAIL))))
                .andExpect(status().isOk());

        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        PasswordResetOtp otp = resetOtpRepository
                .findFirstByUserAndUsedFalseOrderByCreatedAtDesc(user).orElseThrow();

        // Use it once successfully
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", EMAIL,
                                "otpCode", otp.getCode(),
                                "newPassword", "NewPassword123!"
                        ))))
                .andExpect(status().isOk());

        // Try to use the same code again
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", EMAIL,
                                "otpCode", otp.getCode(),
                                "newPassword", "AnotherPassword123!"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // -----------------------------------------------------------------------
    // Wrong OTP code
    // -----------------------------------------------------------------------

    @Test
    void wrongOtpCode_returnsError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("email", EMAIL))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", EMAIL,
                                "otpCode", "000000",
                                "newPassword", "NewPassword123!"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid reset code."));
    }

    // -----------------------------------------------------------------------
    // Expired OTP
    // -----------------------------------------------------------------------

    @Test
    void expiredOtp_returnsError() throws Exception {
        User user = userRepository.findByEmail(EMAIL).orElseThrow();

        PasswordResetOtp expiredOtp = new PasswordResetOtp();
        expiredOtp.setUser(user);
        expiredOtp.setCode("999999");
        expiredOtp.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        expiredOtp.setUsed(false);
        resetOtpRepository.save(expiredOtp);

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", EMAIL,
                                "otpCode", "999999",
                                "newPassword", "NewPassword123!"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Reset code has expired. Please request a new one."));
    }

    // -----------------------------------------------------------------------
    // Unknown email
    // -----------------------------------------------------------------------

    @Test
    void forgotPasswordForUnknownEmail_returnsNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("email", "nobody@example.com"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // -----------------------------------------------------------------------
    // Reset without requesting OTP first
    // -----------------------------------------------------------------------

    @Test
    void resetWithoutRequestingOtpFirst_returnsError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", EMAIL,
                                "otpCode", "123456",
                                "newPassword", "NewPassword123!"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No active reset code found. Please request a new one."));
    }

    // -----------------------------------------------------------------------
    // Password too short fails validation
    // -----------------------------------------------------------------------

    @Test
    void resetWithTooShortPassword_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("email", EMAIL))))
                .andExpect(status().isOk());

        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        PasswordResetOtp otp = resetOtpRepository
                .findFirstByUserAndUsedFalseOrderByCreatedAtDesc(user).orElseThrow();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", EMAIL,
                                "otpCode", otp.getCode(),
                                "newPassword", "short"
                        ))))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // Second forgot-password request invalidates the first OTP
    // -----------------------------------------------------------------------

    @Test
    void secondForgotPasswordRequest_invalidatesFirstOtp() throws Exception {
        // First request
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("email", EMAIL))))
                .andExpect(status().isOk());

        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        PasswordResetOtp firstOtp = resetOtpRepository
                .findFirstByUserAndUsedFalseOrderByCreatedAtDesc(user).orElseThrow();

        // Second request — should invalidate the first
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("email", EMAIL))))
                .andExpect(status().isOk());

        PasswordResetOtp secondOtp = resetOtpRepository
                .findFirstByUserAndUsedFalseOrderByCreatedAtDesc(user).orElseThrow();

        org.junit.jupiter.api.Assertions.assertNotEquals(firstOtp.getId(), secondOtp.getId());

        // Trying the old OTP fails
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", EMAIL,
                                "otpCode", firstOtp.getCode(),
                                "newPassword", "NewPassword123!"
                        ))))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // Works for ADMIN role too (all roles)
    // -----------------------------------------------------------------------

    @Test
    void forgotPasswordWorksForAdminRole() throws Exception {
        String adminEmail = "reset.admin@example.com";
        userRepository.findByEmail(adminEmail).ifPresent(userRepository::delete);

        User admin = new User();
        admin.setFullName("Reset Admin");
        admin.setEmail(adminEmail);
        admin.setPhoneNumber("0782222222");
        admin.setPassword(passwordEncoder.encode("AdminPass123!"));
        admin.setRole(Role.ROLE_ADMIN);
        admin.setEmailVerified(true);
        admin.setStatus(UserStatus.ACTIVE);
        userRepository.save(admin);

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("email", adminEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(adminEmail));

        User savedAdmin = userRepository.findByEmail(adminEmail).orElseThrow();
        PasswordResetOtp otp = resetOtpRepository
                .findFirstByUserAndUsedFalseOrderByCreatedAtDesc(savedAdmin).orElseThrow();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", adminEmail,
                                "otpCode", otp.getCode(),
                                "newPassword", "NewAdminPass123!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", adminEmail,
                                "password", "NewAdminPass123!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }
}
