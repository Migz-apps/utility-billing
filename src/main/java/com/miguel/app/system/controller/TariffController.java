package com.miguel.app.system.controller;

import com.miguel.app.system.dto.request.TariffRequest;
import com.miguel.app.system.dto.request.TariffTierRequest;
import com.miguel.app.system.dto.response.ApiResponse;
import com.miguel.app.system.dto.response.TariffResponse;
import com.miguel.app.system.service.interfaces.TariffService;
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
@RequestMapping("/api/v1/tariffs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "5 — ADMIN — Management", description = "Full system administration: users, customers, meters, tariffs, bill generation.")
public class TariffController {

    private final TariffService tariffService;

    @PostMapping
    @Operation(summary = "Create tariff")
    public ResponseEntity<ApiResponse<TariffResponse>> create(@Valid @RequestBody TariffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<TariffResponse>builder()
                .success(true)
                .message("Tariff created successfully")
                .data(tariffService.create(request))
                .build());
    }

    @GetMapping
    @Operation(summary = "List tariffs")
    public ResponseEntity<ApiResponse<List<TariffResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.<List<TariffResponse>>builder()
                .success(true)
                .message("Tariffs retrieved successfully")
                .data(tariffService.getAll())
                .build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tariff by id")
    public ResponseEntity<ApiResponse<TariffResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<TariffResponse>builder()
                .success(true)
                .message("Tariff retrieved successfully")
                .data(tariffService.getById(id))
                .build());
    }

    @PostMapping("/{id}/tiers")
    @Operation(summary = "Add tier to tariff")
    public ResponseEntity<ApiResponse<TariffResponse>> addTier(@PathVariable Long id, @Valid @RequestBody TariffTierRequest request) {
        return ResponseEntity.ok(ApiResponse.<TariffResponse>builder()
                .success(true)
                .message("Tariff tier added successfully")
                .data(tariffService.addTier(id, request))
                .build());
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate tariff")
    public ResponseEntity<ApiResponse<TariffResponse>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<TariffResponse>builder()
                .success(true)
                .message("Tariff deactivated successfully")
                .data(tariffService.deactivate(id))
                .build());
    }
}
