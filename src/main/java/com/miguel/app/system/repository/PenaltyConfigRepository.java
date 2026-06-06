package com.miguel.app.system.repository;

import com.miguel.app.system.entity.PenaltyConfig;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PenaltyConfigRepository extends JpaRepository<PenaltyConfig, Long> {
    Optional<PenaltyConfig> findFirstByActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToIsNullOrderByIdDesc(LocalDate effectiveDate);
    Optional<PenaltyConfig> findFirstByActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqualOrderByIdDesc(
            LocalDate effectiveDate1, LocalDate effectiveDate2);
}
