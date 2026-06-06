package com.miguel.app.system.service.impl;

import com.miguel.app.system.dto.request.TariffRequest;
import com.miguel.app.system.dto.request.TariffTierRequest;
import com.miguel.app.system.dto.response.TariffResponse;
import com.miguel.app.system.entity.Tariff;
import com.miguel.app.system.entity.TariffTier;
import com.miguel.app.system.enums.TariffType;
import com.miguel.app.system.exception.BusinessRuleException;
import com.miguel.app.system.exception.ResourceNotFoundException;
import com.miguel.app.system.repository.TariffRepository;
import com.miguel.app.system.repository.TariffTierRepository;
import com.miguel.app.system.service.interfaces.TariffService;
import com.miguel.app.system.util.EntityMapper;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TariffServiceImpl implements TariffService {

    private final TariffRepository tariffRepository;
    private final TariffTierRepository tariffTierRepository;

    @Override
    @Transactional
    public TariffResponse create(TariffRequest request) {
        if (request.tariffType() == TariffType.FLAT
                && (request.ratePerUnit() == null || request.ratePerUnit().signum() <= 0)) {
            throw new BusinessRuleException("A FLAT tariff requires a positive rate per unit");
        }
        if (request.effectiveTo() != null && request.effectiveTo().isBefore(request.effectiveFrom())) {
            throw new BusinessRuleException("Effective-to date cannot be before effective-from date");
        }
        // Exam rule: new tariffs apply only to future billing cycles. Reject any backdated start.
        if (request.effectiveFrom() == null || request.effectiveFrom().isBefore(java.time.LocalDate.now())) {
            throw new BusinessRuleException("Effective-from must be today or a future date");
        }
        Tariff tariff = new Tariff();
        tariff.setName(request.name());
        tariff.setMeterType(request.meterType());
        tariff.setTariffType(request.tariffType());
        tariff.setRatePerUnit(request.tariffType() == TariffType.FLAT ? request.ratePerUnit() : null);
        tariff.setVersion(request.version());
        tariff.setEffectiveFrom(request.effectiveFrom());
        tariff.setEffectiveTo(request.effectiveTo());
        tariff.setActive(request.active() == null || request.active());
        return EntityMapper.toTariffResponse(tariffRepository.save(tariff));
    }

    @Override
    public List<TariffResponse> getAll() {
        return tariffRepository.findAll().stream().map(EntityMapper::toTariffResponse).toList();
    }

    @Override
    public TariffResponse getById(Long id) {
        return EntityMapper.toTariffResponse(getTariff(id));
    }

    @Override
    @Transactional
    public TariffResponse addTier(Long tariffId, TariffTierRequest request) {
        Tariff tariff = getTariff(tariffId);
        if (tariff.getTariffType() != TariffType.TIERED) {
            throw new BusinessRuleException("Only tiered tariffs can accept tiers");
        }
        if (request.minUnits() == null || request.minUnits().signum() < 0) {
            throw new BusinessRuleException("Minimum units must be zero or greater");
        }
        if (request.maxUnits() != null && request.maxUnits().compareTo(request.minUnits()) <= 0) {
            throw new BusinessRuleException("Maximum units must be greater than minimum units");
        }
        if (request.ratePerUnit() == null || request.ratePerUnit().signum() <= 0) {
            throw new BusinessRuleException("Tier rate per unit must be positive");
        }
        validateNoOverlap(tariff, request);
        TariffTier tier = new TariffTier();
        tier.setTariff(tariff);
        tier.setMinUnits(request.minUnits());
        tier.setMaxUnits(request.maxUnits());
        tier.setRatePerUnit(request.ratePerUnit());
        tariff.getTiers().add(tier);
        tariffTierRepository.save(tier);
        return EntityMapper.toTariffResponse(tariffRepository.findById(tariffId)
                .orElseThrow(() -> new ResourceNotFoundException("Tariff not found")));
    }

    @Override
    @Transactional
    public TariffResponse deactivate(Long id) {
        Tariff tariff = getTariff(id);
        tariff.setActive(false);
        return EntityMapper.toTariffResponse(tariffRepository.save(tariff));
    }

    private Tariff getTariff(Long id) {
        return tariffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tariff not found"));
    }

    // Rejects a new tier whose [min, max) range intersects any existing tier, so consumption
    // is never double-charged. (maxUnits == null means an unbounded top tier.)
    private void validateNoOverlap(Tariff tariff, TariffTierRequest request) {
        BigDecimal newMin = request.minUnits();
        BigDecimal newMax = request.maxUnits(); // null = +infinity
        for (TariffTier existing : tariff.getTiers()) {
            BigDecimal exMin = existing.getMinUnits();
            BigDecimal exMax = existing.getMaxUnits(); // null = +infinity
            boolean newStartsAfterExisting = exMax != null && newMin.compareTo(exMax) >= 0;
            boolean newEndsBeforeExisting = newMax != null && newMax.compareTo(exMin) <= 0;
            if (!(newStartsAfterExisting || newEndsBeforeExisting)) {
                throw new BusinessRuleException("Tier range overlaps an existing tier on this tariff");
            }
        }
    }
}
