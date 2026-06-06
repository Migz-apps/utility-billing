package com.miguel.app.system.controller;

import com.miguel.app.system.dto.request.ForgotPasswordRequest;
import com.miguel.app.system.dto.request.LoginRequest;
import com.miguel.app.system.dto.request.RegisterRequest;
import com.miguel.app.system.dto.request.ResendVerificationOtpRequest;
import com.miguel.app.system.dto.request.ResetPasswordRequest;
import com.miguel.app.system.dto.request.VerifyEmailOtpRequest;
import com.miguel.app.system.dto.response.ApiResponse;
import com.miguel.app.system.dto.response.AuthResponse;
import com.miguel.app.system.dto.response.OtpDispatchResponse;
import com.miguel.app.system.dto.response.RegistrationResponse;
import com.miguel.app.system.dto.response.UserResponse;
import com.miguel.app.system.service.interfaces.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "1 — Public (no login)", description = "Registration and login — no JWT required.")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a public customer user")
    public ResponseEntity<ApiResponse<RegistrationResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<RegistrationResponse>builder()
                .success(true)
                .message("User registered successfully. Verification OTP sent to email.")
                .data(authService.register(request))
                .build());
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify a customer email address using OTP")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@Valid @RequestBody VerifyEmailOtpRequest request) {
        authService.verifyEmailOtp(request);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .message("Email verified successfully")
                .data("Email verified successfully. You can now login.")
                .build());
    }

    @PostMapping("/resend-verification-otp")
    @Operation(summary = "Resend email verification OTP for an unverified customer user")
    public ResponseEntity<ApiResponse<OtpDispatchResponse>> resendVerificationOtp(
            @Valid @RequestBody ResendVerificationOtpRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.<OtpDispatchResponse>builder()
                .success(true)
                .message("Verification OTP sent successfully")
                .data(authService.resendVerificationOtp(request))
                .build());
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                .success(true)
                .message("Login successful")
                .data(authService.login(request))
                .build());
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Logout the current user and revoke the current JWT token")
    public ResponseEntity<ApiResponse<String>> logout(@RequestHeader("Authorization") String authorizationHeader) {
        authService.logout(authorizationHeader);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .message("Logout successful")
                .data("Token revoked successfully")
                .build());
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get authenticated user profile")
    public ResponseEntity<ApiResponse<UserResponse>> me() {
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .message("Profile retrieved successfully")
                .data(authService.me())
                .build());
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset OTP — works for all roles")
    public ResponseEntity<ApiResponse<OtpDispatchResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(ApiResponse.<OtpDispatchResponse>builder()
                .success(true)
                .message("Password reset code sent to your email address")
                .data(authService.forgotPassword(request))
                .build());
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using OTP code received by email — works for all roles")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .message("Password reset successfully. You can now log in with your new password.")
                .data("Password updated.")
                .build());
    }
}
