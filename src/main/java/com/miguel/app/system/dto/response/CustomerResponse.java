package com.miguel.app.system.dto.response;

import com.miguel.app.system.enums.CustomerStatus;
import java.time.LocalDateTime;

public record CustomerResponse(
        Long id,
        String fullName,
        String nationalId,
        String email,
        String phoneNumber,
        String address,
        CustomerStatus status,
        Long userId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
