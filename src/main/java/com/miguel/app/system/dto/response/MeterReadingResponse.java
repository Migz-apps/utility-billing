package com.miguel.app.system.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MeterReadingResponse(
        Long id,
        Long meterId,
        String meterNumber,
        BigDecimal previousReading,
        BigDecimal currentReading,
        BigDecimal consumption,
        LocalDate readingDate,
        Integer billingMonth,
        Integer billingYear,
        Long capturedById,
        String capturedByName,
        String generatedBillReference,
        LocalDateTime createdAt
) {
}
