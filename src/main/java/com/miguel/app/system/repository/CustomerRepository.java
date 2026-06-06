package com.miguel.app.system.repository;

import com.miguel.app.system.entity.Customer;
import com.miguel.app.system.entity.User;
import com.miguel.app.system.enums.CustomerStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    boolean existsByNationalId(String nationalId);
    boolean existsByEmail(String email);
    boolean existsByUser(User user);
    Optional<Customer> findByUser(User user);
    Optional<Customer> findByUserId(Long userId);
    Page<Customer> findAllByStatus(CustomerStatus status, Pageable pageable);
    Page<Customer> findAllByStatusAndUserIsNotNull(CustomerStatus status, Pageable pageable);
}
