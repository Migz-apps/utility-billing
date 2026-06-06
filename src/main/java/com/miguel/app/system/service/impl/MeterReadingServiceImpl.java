package com.miguel.app.system.service.impl;

import com.miguel.app.system.dto.request.MeterReadingRequest;
import com.miguel.app.system.dto.response.MeterReadingResponse;
import com.miguel.app.system.dto.response.PagedResponse;
import com.miguel.app.system.entity.Customer;
import com.miguel.app.system.entity.Meter;
import com.miguel.app.system.entity.MeterReading;
import com.miguel.app.system.entity.User;
import com.miguel.app.system.enums.CustomerStatus;
import com.miguel.app.system.enums.MeterStatus;
import com.miguel.app.system.enums.UserStatus;
import com.miguel.app.system.exception.BusinessRuleException;
import com.miguel.app.system.exception.ResourceNotFoundException;
import com.miguel.app.system.repository.MeterReadingRepository;
import com.miguel.app.system.repository.MeterRepository;
import com.miguel.app.system.service.interfaces.BillingService;
import com.miguel.app.system.service.interfaces.MeterReadingService;
import com.miguel.app.system.util.EntityMapper;
import com.miguel.app.system.util.PageResponseBuilder;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeterReadingServiceImpl implements MeterReadingService {

    private final MeterRepository meterRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final BillingService billingService;

    // Guards against fat-finger readings producing absurd bills; tune per real meter capacity.
    @Value("${app.reading.max-consumption:1000000}")
    private BigDecimal maxConsumptionPerReading;

    @Override
    @Transactional
    public MeterReadingResponse create(MeterReadingRequest request) {
        Meter meter = meterRepository.findById(request.meterId())
                .orElseThrow(() -> new ResourceNotFoundException("Meter not found"));
        validateMeter(meter, request.readingDate());

        int month = request.readingDate().getMonthValue();
        int year = request.readingDate().getYear();
        if (meterReadingRepository.existsByMeterAndBillingMonthAndBillingYear(meter, month, year)) {
            throw new BusinessRuleException("A reading already exists for this meter in this month");
        }

        BigDecimal previous = meterReadingRepository.findTopByMeterOrderByReadingDateDescIdDesc(meter)
                .map(MeterReading::getCurrentReading)
                .orElse(BigDecimal.ZERO);
        if (request.currentReading().compareTo(previous) <= 0) {
            throw new BusinessRuleException("Current reading must be greater than previous reading");
        }
        BigDecimal consumption = request.currentReading().subtract(previous);
        if (consumption.compareTo(maxConsumptionPerReading) > 0) {
            throw new BusinessRuleException(
                    "Consumption of " + consumption + " units exceeds the allowed maximum of "
                            + maxConsumptionPerReading + " units per reading; please verify the reading");
        }

        User capturedBy = authenticatedUserService.currentUser();
        MeterReading reading = new MeterReading();
        reading.setMeter(meter);
        reading.setPreviousReading(previous);
        reading.setCurrentReading(request.currentReading());
        reading.setConsumption(consumption);
        reading.setReadingDate(request.readingDate());
        reading.setBillingMonth(month);
        reading.setBillingYear(year);
        reading.setCapturedBy(capturedBy);
        MeterReading saved = meterReadingRepository.save(reading);

        billingService.generateBillForReadingId(saved.getId());
        MeterReading refreshed = meterReadingRepository.findById(saved.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Reading not found after save"));
        return EntityMapper.toMeterReadingResponse(refreshed);
    }

    @Override
    public PagedResponse<MeterReadingResponse> getAll(Pageable pageable) {
        return PageResponseBuilder.build(meterReadingRepository.findAll(pageable), "Readings retrieved successfully", EntityMapper::toMeterReadingResponse);
    }

    @Override
    public MeterReadingResponse getById(Long id) {
        return EntityMapper.toMeterReadingResponse(meterReadingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reading not found")));
    }

    @Override
    public PagedResponse<MeterReadingResponse> getByMeter(Long meterId, Pageable pageable) {
        Meter meter = meterRepository.findById(meterId)
                .orElseThrow(() -> new ResourceNotFoundException("Meter not found"));
        return PageResponseBuilder.build(meterReadingRepository.findByMeter(meter, pageable), "Meter readings retrieved successfully", EntityMapper::toMeterReadingResponse);
    }

    @Override
    public PagedResponse<MeterReadingResponse> getMonthly(Integer month, Integer year, Pageable pageable) {
        return PageResponseBuilder.build(
                meterReadingRepository.findByBillingMonthAndBillingYear(month, year, pageable),
                "Monthly readings retrieved successfully",
                EntityMapper::toMeterReadingResponse
        );
    }

    private void validateMeter(Meter meter, LocalDate readingDate) {
        if (readingDate.isAfter(LocalDate.now())) {
            throw new BusinessRuleException("Reading date cannot be in the future");
        }
        if (meter.getStatus() != MeterStatus.ACTIVE) {
            throw new BusinessRuleException("Meter is inactive and cannot receive readings");
        }
        Customer customer = meter.getCustomer();
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new BusinessRuleException("Inactive customer meters cannot receive readings");
        }
        if (customer.getUser() == null || customer.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("Customer must have an active user account before readings can be captured");
        }
    }
}
