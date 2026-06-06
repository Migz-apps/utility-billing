package com.miguel.app.system.controller;

import com.miguel.app.system.dto.response.ApiResponse;
import com.miguel.app.system.dto.response.BillResponse;
import com.miguel.app.system.dto.response.PagedResponse;
import com.miguel.app.system.service.interfaces.BillingService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bills")
@RequiredArgsConstructor
@Tag(name = "4 — FINANCE — Billing Desk", description = "Approve bills, record payments, and view billing data.")
public class BillController {

    private final BillingService billingService;

    @PostMapping("/generate/{readingId}")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
    @Operation(summary = "Generate a bill from a reading id if one does not already exist")
    public ResponseEntity<ApiResponse<BillResponse>> generate(@PathVariable Long readingId) {
        return ResponseEntity.ok(ApiResponse.<BillResponse>builder()
                .success(true)
                .message("Bill generated successfully")
                .data(billingService.generateBillForReadingId(readingId))
                .build());
    }

    @PostMapping("/generate-monthly")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
    @Operation(summary = "Generate bills for all readings in a given month and year")
    public ResponseEntity<ApiResponse<List<BillResponse>>> generateMonthly(@RequestParam Integer month, @RequestParam Integer year) {
        return ResponseEntity.ok(ApiResponse.<List<BillResponse>>builder()
                .success(true)
                .message("Monthly bills generated successfully")
                .data(billingService.generateMonthly(month, year))
                .build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
    @Operation(summary = "List all bills")
    public ResponseEntity<PagedResponse<BillResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(billingService.getAll(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
    @Operation(summary = "Get a bill by id")
    public ResponseEntity<ApiResponse<BillResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<BillResponse>builder()
                .success(true)
                .message("Bill retrieved successfully")
                .data(billingService.getById(id))
                .build());
    }

    @GetMapping("/reference/{billReference}")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
    @Operation(summary = "Get a bill by its bill reference")
    public ResponseEntity<ApiResponse<BillResponse>> getByReference(@PathVariable String billReference) {
        return ResponseEntity.ok(ApiResponse.<BillResponse>builder()
                .success(true)
                .message("Bill retrieved successfully")
                .data(billingService.getByReference(billReference))
                .build());
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
    @Operation(summary = "List bills for a given customer")
    public ResponseEntity<PagedResponse<BillResponse>> getByCustomer(@PathVariable Long customerId, Pageable pageable) {
        return ResponseEntity.ok(billingService.getByCustomer(customerId, pageable));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Hidden
    @Operation(summary = "List the current customer's bills")
    public ResponseEntity<PagedResponse<BillResponse>> getMyBills(Pageable pageable) {
        return ResponseEntity.ok(billingService.getMyBills(pageable));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
    @Operation(summary = "Approve a pending bill")
    public ResponseEntity<ApiResponse<BillResponse>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<BillResponse>builder()
                .success(true)
                .message("Bill approved successfully")
                .data(billingService.approve(id))
                .build());
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
    @Operation(summary = "Cancel a bill that has not been paid")
    public ResponseEntity<ApiResponse<BillResponse>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<BillResponse>builder()
                .success(true)
                .message("Bill cancelled successfully")
                .data(billingService.cancel(id))
                .build());
    }

    @PostMapping("/process-overdue")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
    @Operation(summary = "Process overdue bills and apply penalties where needed")
    public ResponseEntity<ApiResponse<List<BillResponse>>> processOverdue() {
        return ResponseEntity.ok(ApiResponse.<List<BillResponse>>builder()
                .success(true)
                .message("Overdue bills processed successfully")
                .data(billingService.processOverdueBills())
                .build());
    }
}
