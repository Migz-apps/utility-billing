package com.miguel.app.system;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miguel.app.system.dto.request.MeterRequest;
import com.miguel.app.system.enums.MeterType;
import com.miguel.app.system.entity.EmailVerificationOtp;
import com.miguel.app.system.entity.User;
import com.miguel.app.system.enums.Role;
import com.miguel.app.system.enums.UserStatus;
import com.miguel.app.system.repository.EmailVerificationOtpRepository;
import com.miguel.app.system.repository.UserRepository;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.mail.enabled=true")
class AuthOtpFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationOtpRepository otpRepository;

    @MockBean
    private JavaMailSender javaMailSender;

    @Test
    void meterRequestAcceptsCaseInsensitiveEnumValue() throws Exception {
        MeterRequest request = objectMapper.readValue("""
                {
                  "meterNumber": "WTR-2026-0002",
                  "meterType": "Electricity",
                  "installationDate": "2026-06-05",
                  "customerId": 25
                }
                """, MeterRequest.class);

        org.junit.jupiter.api.Assertions.assertEquals(MeterType.ELECTRICITY, request.meterType());
        org.junit.jupiter.api.Assertions.assertEquals(LocalDate.of(2026, 6, 5), request.installationDate());
    }

    @Test
    void registerThenVerifyThenLoginWorks() throws Exception {
        String email = "otp.customer@example.com";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Otp Customer",
                                "email", email,
                                "phoneNumber", "0781234567",
                                "password", "Customer123!"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.role").value("ROLE_CUSTOMER"))
                .andExpect(jsonPath("$.data.verificationRequired").value(true));

        User savedUser = userRepository.findByEmail(email).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(Role.ROLE_CUSTOMER, savedUser.getRole());
        org.junit.jupiter.api.Assertions.assertEquals(UserStatus.INACTIVE, savedUser.getStatus());
        org.junit.jupiter.api.Assertions.assertFalse(savedUser.isEmailVerified());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Customer123!"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email has not been verified"));

        EmailVerificationOtp otp = otpRepository.findFirstByUserAndUsedFalseOrderByCreatedAtDesc(savedUser)
                .orElseThrow();

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "otpCode", otp.getCode()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        User verifiedUser = userRepository.findByEmail(email).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(UserStatus.ACTIVE, verifiedUser.getStatus());
        org.junit.jupiter.api.Assertions.assertTrue(verifiedUser.isEmailVerified());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Customer123!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.email").value(email));
    }
}
