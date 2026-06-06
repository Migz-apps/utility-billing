package com.miguel.app.system.repository;

import com.miguel.app.system.entity.Customer;
import com.miguel.app.system.entity.Bill;
import com.miguel.app.system.entity.Notification;
import com.miguel.app.system.enums.NotificationType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByCustomer(Customer customer, Pageable pageable);

    Optional<Notification> findFirstByBillAndNotificationType(Bill bill, NotificationType notificationType);
}
