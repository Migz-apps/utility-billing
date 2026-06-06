package com.miguel.app.system.entity;

import com.miguel.app.system.enums.PenaltyType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "penalty_configs")
public class PenaltyConfig extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PenaltyType penaltyType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amountOrPercentage;

    @Column(nullable = false)
    private Integer gracePeriodDays;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;
}
