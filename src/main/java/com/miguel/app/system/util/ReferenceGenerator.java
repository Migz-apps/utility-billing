package com.miguel.app.system.util;

import java.time.LocalDate;

public final class ReferenceGenerator {

    private ReferenceGenerator() {
    }

    public static String billReference(LocalDate date, long sequence) {
        return "BILL-%d-%02d-%06d".formatted(date.getYear(), date.getMonthValue(), sequence);
    }

    public static String paymentReference(LocalDate date, long sequence) {
        return "PAY-%d-%02d-%06d".formatted(date.getYear(), date.getMonthValue(), sequence);
    }
}
