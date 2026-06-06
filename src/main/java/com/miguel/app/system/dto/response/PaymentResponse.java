package com.miguel.app.system.dto.response;

import com.miguel.app.system.enums.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        String paymentReference,
        Long billId,
        String billReference,
        Long customerId,
        String customerName,
        BigDecimal amountPaid,
        PaymentMethod paymentMethod,
        LocalDate paymentDate,
        Long recordedById,
        String recordedByName,
        LocalDateTime createdAt
) {
}
