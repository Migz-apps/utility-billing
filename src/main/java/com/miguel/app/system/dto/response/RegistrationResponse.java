package com.miguel.app.system.dto.response;

import java.time.LocalDateTime;

public record RegistrationResponse(
        String email,
        String role,
        boolean verificationRequired,
        LocalDateTime otpExpiresAt
) {
}
