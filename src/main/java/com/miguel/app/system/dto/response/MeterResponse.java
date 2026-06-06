package com.miguel.app.system.dto.response;

import com.miguel.app.system.enums.MeterStatus;
import com.miguel.app.system.enums.MeterType;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MeterResponse(
        Long id,
        String meterNumber,
        MeterType meterType,
        LocalDate installationDate,
        MeterStatus status,
        Long customerId,
        String customerName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
