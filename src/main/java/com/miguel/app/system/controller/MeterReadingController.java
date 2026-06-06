package com.miguel.app.system.controller;

import com.miguel.app.system.dto.request.MeterReadingRequest;
import com.miguel.app.system.dto.response.ApiResponse;
import com.miguel.app.system.dto.response.MeterReadingResponse;
import com.miguel.app.system.dto.response.PagedResponse;
import com.miguel.app.system.service.interfaces.MeterReadingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/readings")
@RequiredArgsConstructor
@Tag(name = "3 — OPERATOR — Field Work", description = "Capture meter readings and view meters in the field.")
public class MeterReadingController {

    private final MeterReadingService meterReadingService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Capture a meter reading and auto-generate its bill")
    public ResponseEntity<ApiResponse<MeterReadingResponse>> create(@Valid @RequestBody MeterReadingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<MeterReadingResponse>builder()
                .success(true)
                .message("Meter reading captured successfully")
                .data(meterReadingService.create(request))
                .build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "List all readings")
    public ResponseEntity<PagedResponse<MeterReadingResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(meterReadingService.getAll(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Get reading by id")
    public ResponseEntity<ApiResponse<MeterReadingResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<MeterReadingResponse>builder()
                .success(true)
                .message("Meter reading retrieved successfully")
                .data(meterReadingService.getById(id))
                .build());
    }

    @GetMapping("/meter/{meterId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Get readings by meter")
    public ResponseEntity<PagedResponse<MeterReadingResponse>> getByMeter(@PathVariable Long meterId, Pageable pageable) {
        return ResponseEntity.ok(meterReadingService.getByMeter(meterId, pageable));
    }

    @GetMapping("/monthly")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Get monthly readings")
    public ResponseEntity<PagedResponse<MeterReadingResponse>> getMonthly(@RequestParam Integer month, @RequestParam Integer year, Pageable pageable) {
        return ResponseEntity.ok(meterReadingService.getMonthly(month, year, pageable));
    }
}
