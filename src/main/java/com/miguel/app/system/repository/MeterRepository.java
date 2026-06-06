package com.miguel.app.system.repository;

import com.miguel.app.system.entity.Customer;
import com.miguel.app.system.entity.Meter;
import com.miguel.app.system.enums.MeterStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeterRepository extends JpaRepository<Meter, Long> {
    boolean existsByMeterNumber(String meterNumber);
    Optional<Meter> findByMeterNumber(String meterNumber);
    List<Meter> findByCustomer(Customer customer);
    Page<Meter> findAllByCustomer(Customer customer, Pageable pageable);
    Page<Meter> findByStatus(MeterStatus status, Pageable pageable);
    boolean existsByCustomerAndStatus(Customer customer, MeterStatus status);
}
