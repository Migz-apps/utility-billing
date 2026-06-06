package com.miguel.app.system.dto.response;

import com.miguel.app.system.enums.MeterType;
import com.miguel.app.system.enums.TariffType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TariffResponse(
        Long id,
        String name,
        MeterType meterType,
        TariffType tariffType,
        BigDecimal ratePerUnit,
        Integer version,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<TariffTierResponse> tiers
) {
}
