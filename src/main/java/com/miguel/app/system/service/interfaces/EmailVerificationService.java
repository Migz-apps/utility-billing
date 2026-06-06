package com.miguel.app.system.service.interfaces;

import com.miguel.app.system.dto.response.OtpDispatchResponse;
import com.miguel.app.system.entity.User;

public interface EmailVerificationService {

    OtpDispatchResponse issueVerificationOtp(User user);

    OtpDispatchResponse resendVerificationOtp(String email);

    void verifyEmailOtp(String email, String otpCode);

    void cleanupExpiredOtps();
}
