package com.miguel.app.system.dto.request;

import com.miguel.app.system.enums.MeterType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

public record FixedChargeRequest(
        @NotNull(message = "Meter type is required")
        MeterType meterType,
        @NotNull(message = "Amount is required")
        @PositiveOrZero(message = "Amount must be zero or positive")
        BigDecimal amount,
        @NotNull(message = "Version is required")
        Integer version,
        @NotNull(message = "Effective from date is required")
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        Boolean active
) {
}
