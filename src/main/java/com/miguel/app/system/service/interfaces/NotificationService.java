package com.miguel.app.system.service.interfaces;

import com.miguel.app.system.dto.response.NotificationResponse;
import com.miguel.app.system.dto.response.PagedResponse;
import com.miguel.app.system.entity.Bill;
import com.miguel.app.system.entity.Customer;
import com.miguel.app.system.enums.NotificationType;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    NotificationResponse create(Customer customer, Bill bill, String message, NotificationType type);
    PagedResponse<NotificationResponse> getAll(Pageable pageable);
    PagedResponse<NotificationResponse> getByCustomer(Long customerId, Pageable pageable);
    PagedResponse<NotificationResponse> getMyNotifications(Pageable pageable);
    NotificationResponse markAsRead(Long id);
}
