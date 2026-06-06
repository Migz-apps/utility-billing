package com.miguel.app.system.controller;

import com.miguel.app.system.dto.request.FixedChargeRequest;
import com.miguel.app.system.dto.request.PenaltyConfigRequest;
import com.miguel.app.system.dto.request.TaxConfigRequest;
import com.miguel.app.system.dto.response.ApiResponse;
import com.miguel.app.system.dto.response.FixedChargeResponse;
import com.miguel.app.system.dto.response.PenaltyConfigResponse;
import com.miguel.app.system.dto.response.TaxConfigResponse;
import com.miguel.app.system.service.interfaces.BillingConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "5 — ADMIN — Management", description = "Full system administration: users, customers, meters, tariffs, bill generation.")
public class BillingConfigController {

    private final BillingConfigService billingConfigService;

    @PostMapping("/fixed-charges")
    @Operation(summary = "Create a fixed charge configuration")
    public ResponseEntity<ApiResponse<FixedChargeResponse>> createFixedCharge(@Valid @RequestBody FixedChargeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<FixedChargeResponse>builder()
                .success(true)
                .message("Fixed charge created successfully")
                .data(billingConfigService.createFixedCharge(request))
                .build());
    }

    @GetMapping("/fixed-charges")
    @Operation(summary = "List all fixed charge configurations")
    public ResponseEntity<ApiResponse<List<FixedChargeResponse>>> getFixedCharges() {
        return ResponseEntity.ok(ApiResponse.<List<FixedChargeResponse>>builder()
                .success(true)
                .message("Fixed charges retrieved successfully")
                .data(billingConfigService.getFixedCharges())
                .build());
    }

    @GetMapping("/fixed-charges/{id}")
    @Operation(summary = "Get a fixed charge configuration by id")
    public ResponseEntity<ApiResponse<FixedChargeResponse>> getFixedCharge(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<FixedChargeResponse>builder()
                .success(true)
                .message("Fixed charge retrieved successfully")
                .data(billingConfigService.getFixedCharge(id))
                .build());
    }

    @PatchMapping("/fixed-charges/{id}/deactivate")
    @Operation(summary = "Deactivate a fixed charge configuration")
    public ResponseEntity<ApiResponse<FixedChargeResponse>> deactivateFixedCharge(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<FixedChargeResponse>builder()
                .success(true)
                .message("Fixed charge deactivated successfully")
                .data(billingConfigService.deactivateFixedCharge(id))
                .build());
    }

    @PostMapping("/taxes")
    @Operation(summary = "Create a tax configuration")
    public ResponseEntity<ApiResponse<TaxConfigResponse>> createTax(@Valid @RequestBody TaxConfigRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<TaxConfigResponse>builder()
                .success(true)
                .message("Tax config created successfully")
                .data(billingConfigService.createTax(request))
                .build());
    }

    @GetMapping("/taxes")
    @Operation(summary = "List all tax configurations")
    public ResponseEntity<ApiResponse<List<TaxConfigResponse>>> getTaxes() {
        return ResponseEntity.ok(ApiResponse.<List<TaxConfigResponse>>builder()
                .success(true)
                .message("Taxes retrieved successfully")
                .data(billingConfigService.getTaxes())
                .build());
    }

    @GetMapping("/taxes/{id}")
    @Operation(summary = "Get a tax configuration by id")
    public ResponseEntity<ApiResponse<TaxConfigResponse>> getTax(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<TaxConfigResponse>builder()
                .success(true)
                .message("Tax config retrieved successfully")
                .data(billingConfigService.getTax(id))
                .build());
    }

    @PatchMapping("/taxes/{id}/deactivate")
    @Operation(summary = "Deactivate a tax configuration")
    public ResponseEntity<ApiResponse<TaxConfigResponse>> deactivateTax(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<TaxConfigResponse>builder()
                .success(true)
                .message("Tax config deactivated successfully")
                .data(billingConfigService.deactivateTax(id))
                .build());
    }

    @PostMapping("/penalties")
    @Operation(summary = "Create a penalty configuration")
    public ResponseEntity<ApiResponse<PenaltyConfigResponse>> createPenalty(@Valid @RequestBody PenaltyConfigRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<PenaltyConfigResponse>builder()
                .success(true)
                .message("Penalty config created successfully")
                .data(billingConfigService.createPenalty(request))
                .build());
    }

    @GetMapping("/penalties")
    @Operation(summary = "List all penalty configurations")
    public ResponseEntity<ApiResponse<List<PenaltyConfigResponse>>> getPenalties() {
        return ResponseEntity.ok(ApiResponse.<List<PenaltyConfigResponse>>builder()
                .success(true)
                .message("Penalty configs retrieved successfully")
                .data(billingConfigService.getPenalties())
                .build());
    }

    @GetMapping("/penalties/{id}")
    @Operation(summary = "Get a penalty configuration by id")
    public ResponseEntity<ApiResponse<PenaltyConfigResponse>> getPenalty(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<PenaltyConfigResponse>builder()
                .success(true)
                .message("Penalty config retrieved successfully")
                .data(billingConfigService.getPenalty(id))
                .build());
    }

    @PatchMapping("/penalties/{id}/deactivate")
    @Operation(summary = "Deactivate a penalty configuration")
    public ResponseEntity<ApiResponse<PenaltyConfigResponse>> deactivatePenalty(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<PenaltyConfigResponse>builder()
                .success(true)
                .message("Penalty config deactivated successfully")
                .data(billingConfigService.deactivatePenalty(id))
                .build());
    }
}
