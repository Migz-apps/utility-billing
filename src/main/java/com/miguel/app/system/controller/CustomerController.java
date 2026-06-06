package com.miguel.app.system.controller;

import com.miguel.app.system.dto.request.AdminConvertUserToCustomerRequest;
import com.miguel.app.system.dto.request.CustomerRequest;
import com.miguel.app.system.dto.response.ApiResponse;
import com.miguel.app.system.dto.response.CustomerResponse;
import com.miguel.app.system.dto.response.PagedResponse;
import com.miguel.app.system.service.interfaces.CustomerService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "5 — ADMIN — Management", description = "Full system administration: users, customers, meters, tariffs, bill generation.")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create customer")
    public ResponseEntity<ApiResponse<CustomerResponse>> create(@Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<CustomerResponse>builder()
                .success(true)
                .message("Customer created successfully")
                .data(customerService.create(request))
                .build());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List customers")
    public ResponseEntity<PagedResponse<CustomerResponse>> getAll(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(customerService.getAll(pageable));
    }

    @GetMapping("/pending-verification")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List customer profiles that are waiting for admin verification")
    public ResponseEntity<PagedResponse<CustomerResponse>> getPendingVerification(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(customerService.getPendingVerification(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get customer by id")
    public ResponseEntity<ApiResponse<CustomerResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<CustomerResponse>builder()
                .success(true)
                .message("Customer retrieved successfully")
                .data(customerService.getById(id))
                .build());
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get current customer profile", tags = "11. Customer - Self Service")
    public ResponseEntity<ApiResponse<CustomerResponse>> getMe() {
        return ResponseEntity.ok(ApiResponse.<CustomerResponse>builder()
                .success(true)
                .message("Customer profile retrieved successfully")
                .data(customerService.getMe())
                .build());
    }

    @PostMapping("/from-user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a customer profile directly from an existing customer user")
    public ResponseEntity<ApiResponse<CustomerResponse>> createFromUser(
            @PathVariable Long userId,
            @Valid @RequestBody AdminConvertUserToCustomerRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<CustomerResponse>builder()
                .success(true)
                .message("Customer profile created successfully and is pending admin verification")
                .data(customerService.createForUser(userId, request))
                .build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update customer")
    public ResponseEntity<ApiResponse<CustomerResponse>> update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(ApiResponse.<CustomerResponse>builder()
                .success(true)
                .message("Customer updated successfully")
                .data(customerService.update(id, request))
                .build());
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Hidden
    @Operation(summary = "Activate customer")
    public ResponseEntity<ApiResponse<CustomerResponse>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<CustomerResponse>builder()
                .success(true)
                .message("Customer activated successfully")
                .data(customerService.activate(id))
                .build());
    }

    @PatchMapping("/user/{userId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate a customer profile using the linked user id")
    public ResponseEntity<ApiResponse<CustomerResponse>> activateByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.<CustomerResponse>builder()
                .success(true)
                .message("Customer activated successfully")
                .data(customerService.activateByUserId(userId))
                .build());
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate customer")
    public ResponseEntity<ApiResponse<CustomerResponse>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<CustomerResponse>builder()
                .success(true)
                .message("Customer deactivated successfully")
                .data(customerService.deactivate(id))
                .build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete or deactivate customer")
    public ResponseEntity<ApiResponse<CustomerResponse>> delete(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<CustomerResponse>builder()
                .success(true)
                .message("Customer deletion processed successfully")
                .data(customerService.delete(id))
                .build());
    }
}
