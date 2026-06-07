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
import com.miguel.app.system.service.interfaces.MailService;
import com.miguel.app.system.service.interfaces.NotificationService;
import com.miguel.app.system.util.EntityMapper;
import com.miguel.app.system.util.PageResponseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final CustomerRepository customerRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final MailService mailService;

    @Override
    @Transactional
    public NotificationResponse create(Customer customer, Bill bill, String message, NotificationType type) {
        if (customer == null || customer.getEmail() == null || customer.getEmail().isBlank()) {
            log.warn("Skipping notification email because customer email is missing for type {}", type);
            return transientResponse(customer, bill, message, type);
        }

        try {
            mailService.sendSystemNotification(
                    customer.getEmail(),
                    customer.getFullName(),
                    subjectFor(type),
                    message
            );
        } catch (RuntimeException ex) {
            // Do not break billing/payment flows because email delivery failed.
            log.error("Failed to send {} notification email to {}", type, customer.getEmail(), ex);
        }

        // Notification delivery is email-first and does not persist new in-app rows.
        return transientResponse(customer, bill, message, type);
    }

    private NotificationResponse transientResponse(Customer customer, Bill bill, String message, NotificationType type) {
        Long customerId = customer == null ? null : customer.getId();
        Long billId = bill == null ? null : bill.getId();
        String billReference = bill == null ? null : bill.getBillReference();
        return new NotificationResponse(
                null,
                customerId,
                billId,
                billReference,
                message,
                type,
                NotificationStatus.UNREAD,
                null
        );
    }

    private String subjectFor(NotificationType type) {
        return switch (type) {
            case BILL_GENERATED -> "Utility bill generated";
            case PAYMENT_CONFIRMED -> "Payment confirmed";
            case BILL_PAID -> "Bill fully paid";
            case BILL_OVERDUE -> "Bill overdue";
        };
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
