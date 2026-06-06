package com.miguel.app.system.dto.request;

import com.miguel.app.system.enums.MeterType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record MeterRequest(
        @NotBlank(message = "Meter number is required")
        String meterNumber,
        @NotNull(message = "Meter type is required")
        MeterType meterType,
        @NotNull(message = "Installation date is required")
        LocalDate installationDate,
        @NotNull(message = "Customer ID is required")
        Long customerId
) {
}
