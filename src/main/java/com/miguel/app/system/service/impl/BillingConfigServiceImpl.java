package com.miguel.app.system.service.impl;

import com.miguel.app.system.dto.request.FixedChargeRequest;
import com.miguel.app.system.dto.request.PenaltyConfigRequest;
import com.miguel.app.system.dto.request.TaxConfigRequest;
import com.miguel.app.system.dto.response.FixedChargeResponse;
import com.miguel.app.system.dto.response.PenaltyConfigResponse;
import com.miguel.app.system.dto.response.TaxConfigResponse;
import com.miguel.app.system.entity.FixedCharge;
import com.miguel.app.system.entity.PenaltyConfig;
import com.miguel.app.system.entity.TaxConfig;
import com.miguel.app.system.exception.BusinessRuleException;
import com.miguel.app.system.exception.ResourceNotFoundException;
import java.time.LocalDate;
import com.miguel.app.system.repository.FixedChargeRepository;
import com.miguel.app.system.repository.PenaltyConfigRepository;
import com.miguel.app.system.repository.TaxConfigRepository;
import com.miguel.app.system.service.interfaces.BillingConfigService;
import com.miguel.app.system.util.EntityMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillingConfigServiceImpl implements BillingConfigService {

    private final FixedChargeRepository fixedChargeRepository;
    private final TaxConfigRepository taxConfigRepository;
    private final PenaltyConfigRepository penaltyConfigRepository;

    @Override
    @Transactional
    public FixedChargeResponse createFixedCharge(FixedChargeRequest request) {
        validateEffectiveWindow(request.effectiveFrom(), request.effectiveTo());
        FixedCharge fixedCharge = new FixedCharge();
        fixedCharge.setMeterType(request.meterType());
        fixedCharge.setAmount(request.amount());
        fixedCharge.setVersion(request.version());
        fixedCharge.setEffectiveFrom(request.effectiveFrom());
        fixedCharge.setEffectiveTo(request.effectiveTo());
        fixedCharge.setActive(request.active() == null || request.active());
        return EntityMapper.toFixedChargeResponse(fixedChargeRepository.save(fixedCharge));
    }

    @Override
    public List<FixedChargeResponse> getFixedCharges() {
        return fixedChargeRepository.findAll().stream().map(EntityMapper::toFixedChargeResponse).toList();
    }

    @Override
    public FixedChargeResponse getFixedCharge(Long id) {
        return EntityMapper.toFixedChargeResponse(getFixedChargeEntity(id));
    }

    @Override
    @Transactional
    public FixedChargeResponse deactivateFixedCharge(Long id) {
        FixedCharge fixedCharge = getFixedChargeEntity(id);
        fixedCharge.setActive(false);
        return EntityMapper.toFixedChargeResponse(fixedChargeRepository.save(fixedCharge));
    }

    @Override
    @Transactional
    public TaxConfigResponse createTax(TaxConfigRequest request) {
        validateEffectiveWindow(request.effectiveFrom(), request.effectiveTo());
        TaxConfig taxConfig = new TaxConfig();
        taxConfig.setName(request.name());
        taxConfig.setPercentage(request.percentage());
        taxConfig.setActive(request.active() == null || request.active());
        taxConfig.setEffectiveFrom(request.effectiveFrom());
        taxConfig.setEffectiveTo(request.effectiveTo());
        return EntityMapper.toTaxConfigResponse(taxConfigRepository.save(taxConfig));
    }

    @Override
    public List<TaxConfigResponse> getTaxes() {
        return taxConfigRepository.findAll().stream().map(EntityMapper::toTaxConfigResponse).toList();
    }

    @Override
    public TaxConfigResponse getTax(Long id) {
        return EntityMapper.toTaxConfigResponse(getTaxEntity(id));
    }

    @Override
    @Transactional
    public TaxConfigResponse deactivateTax(Long id) {
        TaxConfig taxConfig = getTaxEntity(id);
        taxConfig.setActive(false);
        return EntityMapper.toTaxConfigResponse(taxConfigRepository.save(taxConfig));
    }

    @Override
    @Transactional
    public PenaltyConfigResponse createPenalty(PenaltyConfigRequest request) {
        validateEffectiveWindow(request.effectiveFrom(), request.effectiveTo());
        PenaltyConfig penaltyConfig = new PenaltyConfig();
        penaltyConfig.setName(request.name());
        penaltyConfig.setPenaltyType(request.penaltyType());
        penaltyConfig.setAmountOrPercentage(request.amountOrPercentage());
        penaltyConfig.setGracePeriodDays(request.gracePeriodDays());
        penaltyConfig.setActive(request.active() == null || request.active());
        penaltyConfig.setEffectiveFrom(request.effectiveFrom());
        penaltyConfig.setEffectiveTo(request.effectiveTo());
        return EntityMapper.toPenaltyConfigResponse(penaltyConfigRepository.save(penaltyConfig));
    }

    @Override
    public List<PenaltyConfigResponse> getPenalties() {
        return penaltyConfigRepository.findAll().stream().map(EntityMapper::toPenaltyConfigResponse).toList();
    }

    @Override
    public PenaltyConfigResponse getPenalty(Long id) {
        return EntityMapper.toPenaltyConfigResponse(getPenaltyEntity(id));
    }

    @Override
    @Transactional
    public PenaltyConfigResponse deactivatePenalty(Long id) {
        PenaltyConfig penaltyConfig = getPenaltyEntity(id);
        penaltyConfig.setActive(false);
        return EntityMapper.toPenaltyConfigResponse(penaltyConfigRepository.save(penaltyConfig));
    }

    private FixedCharge getFixedChargeEntity(Long id) {
        return fixedChargeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fixed charge not found"));
    }

    private TaxConfig getTaxEntity(Long id) {
        return taxConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tax config not found"));
    }

    private PenaltyConfig getPenaltyEntity(Long id) {
        return penaltyConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Penalty config not found"));
    }

    // Exam rule: new tariff/charge/tax/penalty configs apply only to future billing cycles.
    // Reject backdated effectiveFrom so the time-based resolver cannot pick a new version
    // for past reading dates regardless of version ordering.
    private void validateEffectiveWindow(LocalDate from, LocalDate to) {
        if (from == null || from.isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Effective-from must be today or a future date");
        }
        if (to != null && to.isBefore(from)) {
            throw new BusinessRuleException("Effective-to date cannot be before effective-from date");
        }
    }
}
