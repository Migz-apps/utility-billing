package com.miguel.app.system.repository;

import com.miguel.app.system.entity.TaxConfig;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxConfigRepository extends JpaRepository<TaxConfig, Long> {
    Optional<TaxConfig> findFirstByActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToIsNullOrderByIdDesc(LocalDate effectiveDate);
    Optional<TaxConfig> findFirstByActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqualOrderByIdDesc(
            LocalDate effectiveDate1, LocalDate effectiveDate2);
}
