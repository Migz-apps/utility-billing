package com.miguel.app.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminConvertUserToCustomerRequest(
        @NotBlank(message = "National ID is required")
        @Size(min = 16, max = 16, message = "National ID must be 16 characters")
        String nationalId,
        @NotBlank(message = "Address is required")
        String address
) {
}
