package com.miguel.app.system.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TaxConfigRequest(
        @NotBlank(message = "Tax name is required")
        String name,
        @NotNull(message = "Percentage is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Percentage must be at least 0")
        @DecimalMax(value = "100.0", inclusive = true, message = "Percentage must be at most 100")
        BigDecimal percentage,
        Boolean active,
        @NotNull(message = "Effective from date is required")
        LocalDate effectiveFrom,
        LocalDate effectiveTo
) {
}
