package com.miguel.app.system.dto.response;

public record AuthResponse(
        String token,
        String role,
        String email,
        String fullName
) {
}
