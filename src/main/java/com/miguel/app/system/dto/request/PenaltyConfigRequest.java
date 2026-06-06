package com.miguel.app.system.dto.request;

import com.miguel.app.system.enums.PenaltyType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PenaltyConfigRequest(
        @NotBlank(message = "Penalty name is required")
        String name,
        @NotNull(message = "Penalty type is required")
        PenaltyType penaltyType,
        @NotNull(message = "Amount or percentage is required")
        @PositiveOrZero(message = "Amount or percentage must be zero or positive")
        BigDecimal amountOrPercentage,
        @NotNull(message = "Grace period days are required")
        @Min(value = 0, message = "Grace period days must be zero or positive")
        Integer gracePeriodDays,
        Boolean active,
        @NotNull(message = "Effective from date is required")
        LocalDate effectiveFrom,
        LocalDate effectiveTo
) {
}
