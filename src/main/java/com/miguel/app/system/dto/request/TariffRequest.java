package com.miguel.app.system.dto.request;

import com.miguel.app.system.enums.MeterType;
import com.miguel.app.system.enums.TariffType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TariffRequest(
        @NotBlank(message = "Tariff name is required")
        String name,
        @NotNull(message = "Meter type is required")
        MeterType meterType,
        @NotNull(message = "Tariff type is required")
        TariffType tariffType,
        @Positive(message = "Rate per unit must be positive")
        BigDecimal ratePerUnit,
        @NotNull(message = "Version is required")
        Integer version,
        @NotNull(message = "Effective from date is required")
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        Boolean active
) {
}
