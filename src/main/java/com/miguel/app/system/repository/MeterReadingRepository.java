package com.miguel.app.system.repository;

import com.miguel.app.system.entity.Meter;
import com.miguel.app.system.entity.MeterReading;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeterReadingRepository extends JpaRepository<MeterReading, Long> {
    boolean existsByMeterAndBillingMonthAndBillingYear(Meter meter, Integer billingMonth, Integer billingYear);
    Optional<MeterReading> findTopByMeterOrderByReadingDateDescIdDesc(Meter meter);
    List<MeterReading> findByMeter(Meter meter);
    Page<MeterReading> findByBillingMonthAndBillingYear(Integer billingMonth, Integer billingYear, Pageable pageable);
    Page<MeterReading> findByMeter(Meter meter, Pageable pageable);
}
