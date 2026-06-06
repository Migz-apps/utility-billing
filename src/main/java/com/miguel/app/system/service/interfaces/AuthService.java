package com.miguel.app.system.service.interfaces;

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

public interface AuthService {
    RegistrationResponse register(RegisterRequest request);
    void verifyEmailOtp(VerifyEmailOtpRequest request);
    OtpDispatchResponse resendVerificationOtp(ResendVerificationOtpRequest request);
    AuthResponse login(LoginRequest request);
    void logout(String authorizationHeader);
    UserResponse me();
    OtpDispatchResponse forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
}
