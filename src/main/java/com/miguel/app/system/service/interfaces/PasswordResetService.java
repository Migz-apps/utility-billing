package com.miguel.app.system.service.interfaces;

import com.miguel.app.system.dto.response.OtpDispatchResponse;

public interface PasswordResetService {

    OtpDispatchResponse sendResetOtp(String email);

    void resetPassword(String email, String otpCode, String newPassword);
}
