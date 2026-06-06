package com.miguel.app.system.controller;

import com.miguel.app.system.dto.request.PaymentRequest;
import com.miguel.app.system.dto.response.ApiResponse;
import com.miguel.app.system.dto.response.PagedResponse;
import com.miguel.app.system.dto.response.PaymentResponse;
import com.miguel.app.system.service.interfaces.PaymentService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "4 — FINANCE — Billing Desk", description = "Approve bills, record payments, and view billing data.")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
    @Operation(summary = "Record a payment against an approved bill")
    public ResponseEntity<ApiResponse<PaymentResponse>> create(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<PaymentResponse>builder()
                .success(true)
                .message("Payment recorded successfully")
                .data(paymentService.create(request))
                .build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
    @Operation(summary = "List all payments")
    public ResponseEntity<PagedResponse<PaymentResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(paymentService.getAll(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
    @Operation(summary = "Get a payment by id")
    public ResponseEntity<ApiResponse<PaymentResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<PaymentResponse>builder()
                .success(true)
                .message("Payment retrieved successfully")
                .data(paymentService.getById(id))
                .build());
    }

    @GetMapping("/bill/{billId}")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
    @Operation(summary = "Get all payments recorded for a bill")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getByBill(@PathVariable Long billId) {
        return ResponseEntity.ok(ApiResponse.<List<PaymentResponse>>builder()
                .success(true)
                .message("Bill payments retrieved successfully")
                .data(paymentService.getByBill(billId))
                .build());
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
    @Operation(summary = "List payments for a customer")
    public ResponseEntity<PagedResponse<PaymentResponse>> getByCustomer(@PathVariable Long customerId, Pageable pageable) {
        return ResponseEntity.ok(paymentService.getByCustomer(customerId, pageable));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Hidden
    @Operation(summary = "List the current customer's payments")
    public ResponseEntity<PagedResponse<PaymentResponse>> getMyPayments(Pageable pageable) {
        return ResponseEntity.ok(paymentService.getMyPayments(pageable));
    }
}
