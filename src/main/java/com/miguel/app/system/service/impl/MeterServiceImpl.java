package com.miguel.app.system.service.impl;

import com.miguel.app.system.dto.request.MeterRequest;
import com.miguel.app.system.dto.response.MeterResponse;
import com.miguel.app.system.dto.response.PagedResponse;
import com.miguel.app.system.entity.Customer;
import com.miguel.app.system.entity.Meter;
import com.miguel.app.system.enums.CustomerStatus;
import com.miguel.app.system.enums.MeterStatus;
import com.miguel.app.system.exception.BusinessRuleException;
import com.miguel.app.system.exception.DuplicateResourceException;
import com.miguel.app.system.exception.ResourceNotFoundException;
import com.miguel.app.system.repository.CustomerRepository;
import com.miguel.app.system.repository.MeterRepository;
import com.miguel.app.system.service.interfaces.MeterService;
import com.miguel.app.system.util.EntityMapper;
import com.miguel.app.system.util.PageResponseBuilder;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeterServiceImpl implements MeterService {

    private final MeterRepository meterRepository;
    private final CustomerRepository customerRepository;
    private final AuthenticatedUserService authenticatedUserService;

    @Override
    @Transactional
    public MeterResponse create(MeterRequest request) {
        if (meterRepository.existsByMeterNumber(request.meterNumber())) {
            throw new DuplicateResourceException("Meter number already exists");
        }
        Meter meter = new Meter();
        apply(meter, request);
        meter.setStatus(MeterStatus.ACTIVE);
        return EntityMapper.toMeterResponse(meterRepository.save(meter));
    }

    @Override
    public PagedResponse<MeterResponse> getAll(Pageable pageable) {
        return PageResponseBuilder.build(meterRepository.findAll(pageable), "Meters retrieved successfully", EntityMapper::toMeterResponse);
    }

    @Override
    public PagedResponse<MeterResponse> getActive(Pageable pageable) {
        return PageResponseBuilder.build(meterRepository.findByStatus(MeterStatus.ACTIVE, pageable), "Active meters retrieved successfully", EntityMapper::toMeterResponse);
    }

    @Override
    public MeterResponse getById(Long id) {
        return EntityMapper.toMeterResponse(getMeter(id));
    }

    @Override
    public PagedResponse<MeterResponse> getByCustomer(Long customerId, Pageable pageable) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        return PageResponseBuilder.build(meterRepository.findAllByCustomer(customer, pageable), "Customer meters retrieved successfully", EntityMapper::toMeterResponse);
    }

    @Override
    public PagedResponse<MeterResponse> getMyMeters(Pageable pageable) {
        Customer customer = authenticatedUserService.currentActiveCustomer();
        return PageResponseBuilder.build(meterRepository.findAllByCustomer(customer, pageable), "Meters retrieved successfully", EntityMapper::toMeterResponse);
    }

    @Override
    @Transactional
    public MeterResponse update(Long id, MeterRequest request) {
        Meter meter = getMeter(id);
        if (!meter.getMeterNumber().equals(request.meterNumber()) && meterRepository.existsByMeterNumber(request.meterNumber())) {
            throw new DuplicateResourceException("Meter number already exists");
        }
        apply(meter, request);
        return EntityMapper.toMeterResponse(meterRepository.save(meter));
    }

    @Override
    @Transactional
    public MeterResponse activate(Long id) {
        Meter meter = getMeter(id);
        ensureCustomerActive(meter.getCustomer());
        meter.setStatus(MeterStatus.ACTIVE);
        return EntityMapper.toMeterResponse(meterRepository.save(meter));
    }

    @Override
    @Transactional
    public MeterResponse deactivate(Long id) {
        Meter meter = getMeter(id);
        meter.setStatus(MeterStatus.INACTIVE);
        return EntityMapper.toMeterResponse(meterRepository.save(meter));
    }

    private Meter getMeter(Long id) {
        return meterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meter not found"));
    }

    private void apply(Meter meter, MeterRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        ensureCustomerActive(customer);
        if (request.installationDate().isAfter(LocalDate.now())) {
            throw new BusinessRuleException("Installation date cannot be in the future");
        }
        meter.setMeterNumber(request.meterNumber());
        meter.setMeterType(request.meterType());
        meter.setInstallationDate(request.installationDate());
        meter.setCustomer(customer);
    }

    private void ensureCustomerActive(Customer customer) {
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new BusinessRuleException("Meter cannot be assigned to an inactive customer");
        }
    }
}
