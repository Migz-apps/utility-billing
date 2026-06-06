package com.miguel.app.system.controller;

import com.miguel.app.system.dto.request.CustomerProfileRequest;
import com.miguel.app.system.dto.response.ApiResponse;
import com.miguel.app.system.dto.response.BillResponse;
import com.miguel.app.system.dto.response.CustomerResponse;
import com.miguel.app.system.dto.response.MeterResponse;
import com.miguel.app.system.dto.response.NotificationResponse;
import com.miguel.app.system.dto.response.PagedResponse;
import com.miguel.app.system.dto.response.PaymentResponse;
import com.miguel.app.system.service.interfaces.BillingService;
import com.miguel.app.system.service.interfaces.CustomerService;
import com.miguel.app.system.service.interfaces.MeterService;
import com.miguel.app.system.service.interfaces.NotificationService;
import com.miguel.app.system.service.interfaces.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers/me")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "2 — CUSTOMER — My Account", description = "Self-service APIs for customers: profile, bills, payments…")
public class CustomerSelfServiceController {

    private final CustomerService customerService;
    private final MeterService meterService;
    private final BillingService billingService;
    private final PaymentService paymentService;
    private final NotificationService notificationService;

    @PostMapping("/profile")
    @Operation(summary = "Create the current customer's profile for admin verification")
    public ResponseEntity<ApiResponse<CustomerResponse>> createProfile(@Valid @RequestBody CustomerProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.<CustomerResponse>builder()
                .success(true)
                .message("Customer profile created successfully and is pending admin verification")
                .data(customerService.createMyProfile(request))
                .build());
    }

    @GetMapping("/meters")
    @Operation(summary = "Get the current customer's assigned meters")
    public ResponseEntity<PagedResponse<MeterResponse>> meters(Pageable pageable) {
        return ResponseEntity.ok(meterService.getMyMeters(pageable));
    }

    @GetMapping("/bills")
    @Operation(summary = "Get the current customer's visible bills")
    public ResponseEntity<PagedResponse<BillResponse>> bills(Pageable pageable) {
        return ResponseEntity.ok(billingService.getMyBills(pageable));
    }

    @GetMapping("/payments")
    @Operation(summary = "Get the current customer's payments")
    public ResponseEntity<PagedResponse<PaymentResponse>> payments(Pageable pageable) {
        return ResponseEntity.ok(paymentService.getMyPayments(pageable));
    }

    @GetMapping("/notifications")
    @Operation(summary = "Get the current customer's notifications")
    public ResponseEntity<PagedResponse<NotificationResponse>> notifications(Pageable pageable) {
        return ResponseEntity.ok(notificationService.getMyNotifications(pageable));
    }

    @PatchMapping("/notifications/{id}/read")
    @Operation(summary = "Mark one of the current customer's notifications as read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markNotificationRead(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<NotificationResponse>builder()
                .success(true)
                .message("Notification marked as read")
                .data(notificationService.markAsRead(id))
                .build());
    }
}
