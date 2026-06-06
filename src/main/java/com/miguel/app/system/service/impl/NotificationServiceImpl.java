package com.miguel.app.system.service.impl;

import com.miguel.app.system.dto.response.NotificationResponse;
import com.miguel.app.system.dto.response.PagedResponse;
import com.miguel.app.system.entity.Bill;
import com.miguel.app.system.entity.Customer;
import com.miguel.app.system.entity.Notification;
import com.miguel.app.system.entity.User;
import com.miguel.app.system.enums.NotificationStatus;
import com.miguel.app.system.enums.NotificationType;
import com.miguel.app.system.enums.Role;
import com.miguel.app.system.exception.ForbiddenException;
import com.miguel.app.system.exception.ResourceNotFoundException;
import com.miguel.app.system.repository.CustomerRepository;
import com.miguel.app.system.repository.NotificationRepository;
import com.miguel.app.system.service.interfaces.NotificationService;
import com.miguel.app.system.util.EntityMapper;
import com.miguel.app.system.util.PageResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final CustomerRepository customerRepository;
    private final AuthenticatedUserService authenticatedUserService;

    @Override
    @Transactional
    public NotificationResponse create(Customer customer, Bill bill, String message, NotificationType type) {
        if (bill != null) {
            return notificationRepository.findFirstByBillAndNotificationType(bill, type)
                    .map(EntityMapper::toNotificationResponse)
                    .orElseGet(() -> saveNotification(customer, bill, message, type));
        }
        return saveNotification(customer, bill, message, type);
    }

    private NotificationResponse saveNotification(Customer customer, Bill bill, String message, NotificationType type) {
        Notification notification = new Notification();
        notification.setCustomer(customer);
        notification.setBill(bill);
        notification.setMessage(message);
        notification.setNotificationType(type);
        notification.setStatus(NotificationStatus.UNREAD);
        return EntityMapper.toNotificationResponse(notificationRepository.save(notification));
    }

    @Override
    public PagedResponse<NotificationResponse> getAll(Pageable pageable) {
        return PageResponseBuilder.build(notificationRepository.findAll(pageable), "Notifications retrieved successfully", EntityMapper::toNotificationResponse);
    }

    @Override
    public PagedResponse<NotificationResponse> getByCustomer(Long customerId, Pageable pageable) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        return PageResponseBuilder.build(notificationRepository.findByCustomer(customer, pageable), "Customer notifications retrieved successfully", EntityMapper::toNotificationResponse);
    }

    @Override
    public PagedResponse<NotificationResponse> getMyNotifications(Pageable pageable) {
        Customer customer = authenticatedUserService.currentActiveCustomer();
        return PageResponseBuilder.build(notificationRepository.findByCustomer(customer, pageable), "Notifications retrieved successfully", EntityMapper::toNotificationResponse);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        User user = authenticatedUserService.currentUser();
        if (user.getRole() == Role.ROLE_CUSTOMER) {
            Customer customer = authenticatedUserService.currentActiveCustomer();
            if (!notification.getCustomer().getId().equals(customer.getId())) {
                throw new ForbiddenException("You cannot modify another customer's notifications");
            }
        }
        notification.setStatus(NotificationStatus.READ);
        return EntityMapper.toNotificationResponse(notificationRepository.save(notification));
    }
}
