package com.miguel.app.system.repository;

import com.miguel.app.system.entity.Tariff;
import com.miguel.app.system.enums.MeterType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TariffRepository extends JpaRepository<Tariff, Long> {
    List<Tariff> findByMeterTypeOrderByVersionDesc(MeterType meterType);
    Optional<Tariff> findFirstByMeterTypeAndActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToIsNullOrderByVersionDesc(
            MeterType meterType, LocalDate effectiveDate);
    Optional<Tariff> findFirstByMeterTypeAndActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqualOrderByVersionDesc(
            MeterType meterType, LocalDate effectiveDate1, LocalDate effectiveDate2);
}
