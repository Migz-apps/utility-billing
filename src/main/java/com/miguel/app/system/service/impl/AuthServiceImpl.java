package com.miguel.app.system.service.impl;

import com.miguel.app.system.dto.request.ForgotPasswordRequest;
import com.miguel.app.system.dto.request.LoginRequest;
import com.miguel.app.system.dto.request.RegisterRequest;
import com.miguel.app.system.dto.request.ResendVerificationOtpRequest;
import com.miguel.app.system.dto.request.ResetPasswordRequest;
import com.miguel.app.system.dto.request.VerifyEmailOtpRequest;
import com.miguel.app.system.dto.response.AuthResponse;
import com.miguel.app.system.dto.response.OtpDispatchResponse;
import com.miguel.app.system.dto.response.RegistrationResponse;
import com.miguel.app.system.dto.response.UserResponse;
import com.miguel.app.system.entity.User;
import com.miguel.app.system.enums.Role;
import com.miguel.app.system.enums.UserStatus;
import com.miguel.app.system.exception.DuplicateResourceException;
import com.miguel.app.system.exception.UnauthorizedException;
import com.miguel.app.system.repository.UserRepository;
import com.miguel.app.system.security.JwtService;
import com.miguel.app.system.service.interfaces.AuthService;
import com.miguel.app.system.service.interfaces.EmailVerificationService;
import com.miguel.app.system.service.interfaces.PasswordResetService;
import com.miguel.app.system.service.interfaces.TokenRevocationService;
import com.miguel.app.system.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final TokenRevocationService tokenRevocationService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;
    private final AuthenticatedUserService authenticatedUserService;

    @Override
    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("A user with this email already exists");
        }

        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(email);
        user.setPhoneNumber(request.phoneNumber());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.ROLE_CUSTOMER);
        user.setEmailVerified(false);
        user.setStatus(UserStatus.INACTIVE);
        User saved = userRepository.save(user);

        OtpDispatchResponse otpDispatch = emailVerificationService.issueVerificationOtp(saved);
        return new RegistrationResponse(
                saved.getEmail(),
                saved.getRole().name(),
                true,
                otpDispatch.otpExpiresAt()
        );
    }

    @Override
    @Transactional
    public void verifyEmailOtp(VerifyEmailOtpRequest request) {
        emailVerificationService.verifyEmailOtp(request.email(), request.otpCode());
    }

    @Override
    @Transactional
    public OtpDispatchResponse resendVerificationOtp(ResendVerificationOtpRequest request) {
        return emailVerificationService.resendVerificationOtp(request.email());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = request.email() == null ? null : request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!user.isEmailVerified()) {
            throw new UnauthorizedException("Email has not been verified");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Account is inactive");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password())
        );

        return new AuthResponse(
                jwtService.generateToken(user),
                user.getRole().name(),
                user.getEmail(),
                user.getFullName()
        );
    }

    @Override
    @Transactional
    public void logout(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Authorization header with Bearer token is required");
        }

        String token = authorizationHeader.substring(7).trim();
        if (token.isEmpty()) {
            throw new UnauthorizedException("Authorization header with Bearer token is required");
        }

        tokenRevocationService.revoke(token);
    }

    @Override
    public UserResponse me() {
        return EntityMapper.toUserResponse(authenticatedUserService.currentUser());
    }

    @Override
    @Transactional
    public OtpDispatchResponse forgotPassword(ForgotPasswordRequest request) {
        return passwordResetService.sendResetOtp(normalizeEmail(request.email()));
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        passwordResetService.resetPassword(
                normalizeEmail(request.email()),
                request.otpCode(),
                request.newPassword()
        );
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
