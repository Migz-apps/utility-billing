package com.miguel.app.system.dto.response;

import com.miguel.app.system.enums.Role;
import com.miguel.app.system.enums.CustomerStatus;
import com.miguel.app.system.enums.UserStatus;
import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        String phoneNumber,
        Role role,
        boolean emailVerified,
        UserStatus status,
        Long customerId,
        CustomerStatus customerStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
