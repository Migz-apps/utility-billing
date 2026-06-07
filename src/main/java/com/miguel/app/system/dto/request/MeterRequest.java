package com.miguel.app.system.dto.request;

import com.miguel.app.system.enums.MeterType;
import io.swagger.v3.oas.annotations.media.Schema;
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
        @Schema(description = "Customer profile id. If you only have a customer-role user id, this endpoint also resolves the linked customer profile automatically.")
        Long customerId
) {
}
