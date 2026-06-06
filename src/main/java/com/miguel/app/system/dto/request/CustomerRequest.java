package com.miguel.app.system.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank(message = "Full name is required")
        String fullName,
        @NotBlank(message = "National ID is required")
        @Size(min = 16, max = 16, message = "National ID must be 16 characters")
        String nationalId,
        @Email(message = "Email must be valid")
        String email,
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^(07\\d{8}|\\+2507\\d{8})$", message = "Phone number must be a valid Rwanda mobile number")
        String phoneNumber,
        @NotBlank(message = "Address is required")
        String address,
        Long userId
) {
}
