package com.miguel.app.system.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TaxConfigResponse(
        Long id,
        String name,
        BigDecimal percentage,
        boolean active,
        LocalDate effectiveFrom,
        LocalDate effectiveTo
) {
}
