package com.miguel.app.system.controller;

import com.miguel.app.system.dto.request.MeterRequest;
import com.miguel.app.system.dto.response.ApiResponse;
import com.miguel.app.system.dto.response.MeterResponse;
import com.miguel.app.system.dto.response.PagedResponse;
import com.miguel.app.system.service.interfaces.MeterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "5 — ADMIN — Management", description = "Full system administration: users, customers, meters, tariffs, bill generation.")
public class MeterController {

    private final MeterService meterService;

    @PostMapping("/meters")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create meter")
    public ResponseEntity<ApiResponse<MeterResponse>> create(@Valid @RequestBody MeterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<MeterResponse>builder()
                .success(true)
                .message("Meter created successfully")
                .data(meterService.create(request))
                .build());
    }

    @GetMapping("/meters")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "List meters")
    public ResponseEntity<PagedResponse<MeterResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(meterService.getAll(pageable));
    }

    @GetMapping("/meters/active")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "List active meters")
    public ResponseEntity<PagedResponse<MeterResponse>> getActive(Pageable pageable) {
        return ResponseEntity.ok(meterService.getActive(pageable));
    }

    @GetMapping("/meters/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Get meter by id")
    public ResponseEntity<ApiResponse<MeterResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<MeterResponse>builder()
                .success(true)
                .message("Meter retrieved successfully")
                .data(meterService.getById(id))
                .build());
    }

    @GetMapping("/customers/{customerId}/meters")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get meters by customer id")
    public ResponseEntity<PagedResponse<MeterResponse>> getByCustomer(@PathVariable Long customerId, Pageable pageable) {
        return ResponseEntity.ok(meterService.getByCustomer(customerId, pageable));
    }

    @PutMapping("/meters/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update meter")
    public ResponseEntity<ApiResponse<MeterResponse>> update(@PathVariable Long id, @Valid @RequestBody MeterRequest request) {
        return ResponseEntity.ok(ApiResponse.<MeterResponse>builder()
                .success(true)
                .message("Meter updated successfully")
                .data(meterService.update(id, request))
                .build());
    }

    @PatchMapping("/meters/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate meter")
    public ResponseEntity<ApiResponse<MeterResponse>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<MeterResponse>builder()
                .success(true)
                .message("Meter activated successfully")
                .data(meterService.activate(id))
                .build());
    }

    @PatchMapping("/meters/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate meter")
    public ResponseEntity<ApiResponse<MeterResponse>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<MeterResponse>builder()
                .success(true)
                .message("Meter deactivated successfully")
                .data(meterService.deactivate(id))
                .build());
    }
}
