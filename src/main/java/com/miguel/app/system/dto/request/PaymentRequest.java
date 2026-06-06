package com.miguel.app.system.dto.request;

import com.miguel.app.system.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentRequest(
        @NotBlank(message = "Bill reference is required")
        String billReference,
        @NotNull(message = "Payment amount is required")
        @Positive(message = "Payment amount must be greater than zero")
        BigDecimal amountPaid,
        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,
        @NotNull(message = "Payment date is required")
        @PastOrPresent(message = "Payment date cannot be in the future")
        LocalDate paymentDate
) {
}
