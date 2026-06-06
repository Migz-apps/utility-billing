package com.miguel.app.system.dto.request;

import com.miguel.app.system.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateStaffUserRequest(
        @NotBlank(message = "Full name is required")
        String fullName,
        @Email(message = "Email must be valid")
        @NotBlank(message = "Email is required")
        String email,
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^(07\\d{8}|\\+2507\\d{8})$", message = "Phone number must be a valid Rwanda mobile number")
        String phoneNumber,
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,
        @NotNull(message = "Role is required")
        Role role
) {
}
