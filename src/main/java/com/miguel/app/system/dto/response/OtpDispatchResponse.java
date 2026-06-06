package com.miguel.app.system.dto.response;

import java.time.LocalDateTime;

public record OtpDispatchResponse(
        String email,
        LocalDateTime otpExpiresAt
) {
}
