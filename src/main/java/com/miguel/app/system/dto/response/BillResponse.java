package com.miguel.app.system.dto.response;

import com.miguel.app.system.enums.BillStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record BillResponse(
        Long id,
        String billReference,
        Long customerId,
        String customerName,
        Long meterId,
        String meterNumber,
        Long meterReadingId,
        Integer billingMonth,
        Integer billingYear,
        BigDecimal consumption,
        BigDecimal tariffAmount,
        BigDecimal fixedCharge,
        BigDecimal taxAmount,
        BigDecimal penaltyAmount,
        BigDecimal totalAmount,
        BigDecimal amountPaid,
        BigDecimal outstandingBalance,
        BillStatus status,
        LocalDate dueDate,
        Long approvedById,
        String approvedByName,
        LocalDateTime approvedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
