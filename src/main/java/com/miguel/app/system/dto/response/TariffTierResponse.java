package com.miguel.app.system.dto.response;

import java.math.BigDecimal;

public record TariffTierResponse(
        Long id,
        BigDecimal minUnits,
        BigDecimal maxUnits,
        BigDecimal ratePerUnit
) {
}
