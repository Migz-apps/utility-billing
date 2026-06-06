package com.miguel.app.system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "meter_readings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"meter_id", "billingMonth", "billingYear"})
)
public class MeterReading extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meter_id", nullable = false)
    private Meter meter;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal previousReading;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal currentReading;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal consumption;

    @Column(nullable = false)
    private LocalDate readingDate;

    @Column(nullable = false)
    private Integer billingMonth;

    @Column(nullable = false)
    private Integer billingYear;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "captured_by", nullable = false)
    private User capturedBy;

    @OneToOne(mappedBy = "meterReading")
    private Bill bill;
}
