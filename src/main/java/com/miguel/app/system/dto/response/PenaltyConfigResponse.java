package com.miguel.app.system.dto.response;

import com.miguel.app.system.enums.PenaltyType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PenaltyConfigResponse(
        Long id,
        String name,
        PenaltyType penaltyType,
        BigDecimal amountOrPercentage,
        Integer gracePeriodDays,
        boolean active,
        LocalDate effectiveFrom,
        LocalDate effectiveTo
) {
}
