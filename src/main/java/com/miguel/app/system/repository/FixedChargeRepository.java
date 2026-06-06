package com.miguel.app.system.repository;

import com.miguel.app.system.entity.FixedCharge;
import com.miguel.app.system.enums.MeterType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FixedChargeRepository extends JpaRepository<FixedCharge, Long> {
    Optional<FixedCharge> findFirstByMeterTypeAndActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToIsNullOrderByVersionDesc(
            MeterType meterType, LocalDate effectiveDate);
    Optional<FixedCharge> findFirstByMeterTypeAndActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqualOrderByVersionDesc(
            MeterType meterType, LocalDate effectiveDate1, LocalDate effectiveDate2);
}
