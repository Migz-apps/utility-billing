package com.miguel.app.system.service.impl;

import com.miguel.app.system.dto.response.BillResponse;
import com.miguel.app.system.dto.response.PagedResponse;
import com.miguel.app.system.entity.Bill;
import com.miguel.app.system.entity.Customer;
import com.miguel.app.system.entity.FixedCharge;
import com.miguel.app.system.entity.Meter;
import com.miguel.app.system.entity.MeterReading;
import com.miguel.app.system.entity.PenaltyConfig;
import com.miguel.app.system.entity.Tariff;
import com.miguel.app.system.entity.TariffTier;
import com.miguel.app.system.entity.TaxConfig;
import com.miguel.app.system.entity.User;
import com.miguel.app.system.enums.BillStatus;
import com.miguel.app.system.enums.CustomerStatus;
import com.miguel.app.system.enums.MeterStatus;
import com.miguel.app.system.enums.NotificationType;
import com.miguel.app.system.enums.UserStatus;
import com.miguel.app.system.exception.BusinessRuleException;
import com.miguel.app.system.exception.ResourceNotFoundException;
import com.miguel.app.system.repository.BillRepository;
import com.miguel.app.system.repository.FixedChargeRepository;
import com.miguel.app.system.repository.MeterReadingRepository;
import com.miguel.app.system.repository.PenaltyConfigRepository;
import com.miguel.app.system.repository.TariffRepository;
import com.miguel.app.system.repository.TariffTierRepository;
import com.miguel.app.system.repository.TaxConfigRepository;
import com.miguel.app.system.service.interfaces.BillingService;
import com.miguel.app.system.service.interfaces.NotificationService;
import com.miguel.app.system.util.EntityMapper;
import com.miguel.app.system.util.PageResponseBuilder;
import com.miguel.app.system.util.ReferenceGenerator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillingServiceImpl implements BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingServiceImpl.class);

    private final BillRepository billRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final TariffRepository tariffRepository;
    private final TariffTierRepository tariffTierRepository;
    private final FixedChargeRepository fixedChargeRepository;
    private final TaxConfigRepository taxConfigRepository;
    private final PenaltyConfigRepository penaltyConfigRepository;
    private final NotificationService notificationService;
    private final AuthenticatedUserService authenticatedUserService;
    private final OverdueBillProcessor overdueBillProcessor;

    @Value("${app.billing.due-days:15}")
    private int dueDays;

    @Override
    @Transactional
    public BillResponse generateBillForReadingId(Long readingId) {
        MeterReading reading = meterReadingRepository.findById(readingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meter reading not found"));
        return EntityMapper.toBillResponse(generateBillIfMissing(reading));
    }

    @Override
    @Transactional
    public List<BillResponse> generateMonthly(Integer month, Integer year) {
        return meterReadingRepository.findByBillingMonthAndBillingYear(month, year, Pageable.unpaged())
                .getContent()
                .stream()
                .map(this::generateBillIfMissing)
                .map(EntityMapper::toBillResponse)
                .toList();
    }

    @Override
    public PagedResponse<BillResponse> getAll(Pageable pageable) {
        return PageResponseBuilder.build(billRepository.findAll(pageable), "Bills retrieved successfully", EntityMapper::toBillResponse);
    }

    @Override
    public BillResponse getById(Long id) {
        return EntityMapper.toBillResponse(getBill(id));
    }

    @Override
    public BillResponse getByReference(String billReference) {
        Bill bill = billRepository.findByBillReference(billReference)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found"));
        return EntityMapper.toBillResponse(bill);
    }

    @Override
    public PagedResponse<BillResponse> getByCustomer(Long customerId, Pageable pageable) {
        Customer customer = new Customer();
        customer.setId(customerId);
        return PageResponseBuilder.build(billRepository.findByCustomer(customer, pageable), "Customer bills retrieved successfully", EntityMapper::toBillResponse);
    }

    @Override
    public PagedResponse<BillResponse> getMyBills(Pageable pageable) {
        Customer customer = authenticatedUserService.currentActiveCustomer();
        return PageResponseBuilder.build(
                billRepository.findByCustomerAndStatusIn(
                        customer,
                        List.of(BillStatus.APPROVED, BillStatus.PARTIALLY_PAID, BillStatus.PAID, BillStatus.OVERDUE),
                        pageable
                ),
                "Bills retrieved successfully",
                EntityMapper::toBillResponse
        );
    }

    @Override
    @Transactional
    public BillResponse approve(Long id) {
        Bill bill = getBill(id);
        if (bill.getStatus() != BillStatus.PENDING_APPROVAL && bill.getStatus() != BillStatus.DRAFT) {
            throw new BusinessRuleException("Only pending bills can be approved");
        }
        User user = authenticatedUserService.currentUser();
        bill.setStatus(BillStatus.APPROVED);
        bill.setApprovedBy(user);
        bill.setApprovedAt(java.time.LocalDateTime.now());
        return EntityMapper.toBillResponse(billRepository.save(bill));
    }

    @Override
    @Transactional
    public BillResponse cancel(Long id) {
        Bill bill = getBill(id);
        if (bill.getStatus() == BillStatus.PAID || bill.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException("Paid or partially paid bills cannot be cancelled");
        }
        bill.setStatus(BillStatus.CANCELLED);
        return EntityMapper.toBillResponse(billRepository.save(bill));
    }

    @Override
    public List<BillResponse> processOverdueBills() {
        LocalDate today = LocalDate.now();
        PenaltyConfig penaltyConfig = resolvePenalty(today);
        // Respect the configured grace period: a bill is overdue only after dueDate + grace days.
        int graceDays = penaltyConfig != null ? penaltyConfig.getGracePeriodDays() : 0;
        LocalDate cutoff = today.minusDays(graceDays);

        List<Bill> bills = billRepository.findByStatusInAndDueDateBefore(
                List.of(BillStatus.APPROVED, BillStatus.PARTIALLY_PAID), cutoff);
        List<BillResponse> responses = new ArrayList<>();
        for (Bill bill : bills) {
            try {
                // Each bill runs in its own transaction; one failure does not sink the batch.
                responses.add(overdueBillProcessor.applyOverdue(bill.getId(), penaltyConfig));
            } catch (RuntimeException ex) {
                log.error("Failed to process overdue bill {}: {}", bill.getId(), ex.getMessage());
            }
        }
        return responses;
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void scheduledOverdueProcessing() {
        processOverdueBills();
    }

    @Transactional
    protected Bill generateBillIfMissing(MeterReading reading) {
        return billRepository.findByMeterReading(reading)
                .orElseGet(() -> createBill(reading));
    }

    private Bill createBill(MeterReading reading) {
        Meter meter = reading.getMeter();
        Customer customer = meter.getCustomer();
        if (meter.getStatus() != MeterStatus.ACTIVE) {
            throw new BusinessRuleException("Inactive meter cannot receive bills");
        }
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new BusinessRuleException("Inactive customers cannot receive bills");
        }
        if (customer.getUser() == null || customer.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("Customer must have an active user account before bills can be generated");
        }
        if (billRepository.existsByMeterAndBillingMonthAndBillingYear(meter, reading.getBillingMonth(), reading.getBillingYear())) {
            throw new BusinessRuleException("A bill already exists for this meter in this month");
        }

        Tariff tariff = resolveTariff(meter.getMeterType(), reading.getReadingDate());
        FixedCharge fixedCharge = resolveFixedCharge(meter.getMeterType(), reading.getReadingDate());
        TaxConfig taxConfig = resolveTax(reading.getReadingDate());

        BigDecimal tariffAmount = calculateTariffAmount(reading.getConsumption(), tariff);
        BigDecimal fixedChargeAmount = fixedCharge != null ? fixedCharge.getAmount() : BigDecimal.ZERO;
        BigDecimal subtotal = tariffAmount.add(fixedChargeAmount);
        BigDecimal taxAmount = taxConfig != null
                ? subtotal.multiply(taxConfig.getPercentage()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.add(taxAmount);

        Bill bill = new Bill();
        // Reference is derived from the (already-persisted, 1:1) reading id, so it is unique and
        // final at INSERT time. Avoids a two-phase TEMP save and lets DB triggers see the real reference.
        bill.setBillReference(ReferenceGenerator.billReference(reading.getReadingDate(), reading.getId()));
        bill.setCustomer(customer);
        bill.setMeter(meter);
        bill.setMeterReading(reading);
        bill.setBillingMonth(reading.getBillingMonth());
        bill.setBillingYear(reading.getBillingYear());
        bill.setConsumption(reading.getConsumption());
        bill.setTariffAmount(tariffAmount);
        bill.setFixedCharge(fixedChargeAmount);
        bill.setTaxAmount(taxAmount);
        bill.setPenaltyAmount(BigDecimal.ZERO);
        bill.setTotalAmount(totalAmount);
        bill.setAmountPaid(BigDecimal.ZERO);
        bill.setOutstandingBalance(totalAmount);
        bill.setStatus(BillStatus.PENDING_APPROVAL);
        bill.setDueDate(reading.getReadingDate().plusDays(dueDays));
        Bill finalBill = billRepository.save(bill);
        notificationService.create(finalBill.getCustomer(), finalBill, billGeneratedMessage(finalBill), NotificationType.BILL_GENERATED);
        return finalBill;
    }

    private BigDecimal calculateTariffAmount(BigDecimal consumption, Tariff tariff) {
        if (tariff.getTariffType() == com.miguel.app.system.enums.TariffType.FLAT) {
            return consumption.multiply(tariff.getRatePerUnit()).setScale(2, RoundingMode.HALF_UP);
        }

        List<TariffTier> tiers = tariffTierRepository.findByTariffOrderByMinUnitsAsc(tariff);
        if (tiers.isEmpty()) {
            throw new BusinessRuleException("Tiered tariff has no tiers configured");
        }
        if (tiers.get(0).getMinUnits().signum() != 0) {
            throw new BusinessRuleException("Tiered tariff must start at 0 units");
        }
        for (int i = 1; i < tiers.size(); i++) {
            BigDecimal prevMax = tiers.get(i - 1).getMaxUnits();
            if (prevMax == null || prevMax.compareTo(tiers.get(i).getMinUnits()) != 0) {
                throw new BusinessRuleException("Tiered tariff has a gap or overlap between tiers");
            }
        }
        BigDecimal total = BigDecimal.ZERO;
        for (TariffTier tier : tiers) {
            if (consumption.compareTo(tier.getMinUnits()) <= 0) {
                continue;
            }
            BigDecimal tierUpper = tier.getMaxUnits() == null ? consumption : consumption.min(tier.getMaxUnits());
            BigDecimal units = tierUpper.subtract(tier.getMinUnits());
            if (units.compareTo(BigDecimal.ZERO) > 0) {
                total = total.add(units.multiply(tier.getRatePerUnit()));
            }
            if (tier.getMaxUnits() == null || consumption.compareTo(tier.getMaxUnits()) <= 0) {
                break;
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private Tariff resolveTariff(com.miguel.app.system.enums.MeterType meterType, LocalDate date) {
        return tariffRepository.findFirstByMeterTypeAndActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToIsNullOrderByVersionDesc(meterType, date)
                .or(() -> tariffRepository.findFirstByMeterTypeAndActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqualOrderByVersionDesc(meterType, date, date))
                .orElseThrow(() -> new BusinessRuleException("No active tariff found for this meter type and billing period"));
    }

    private FixedCharge resolveFixedCharge(com.miguel.app.system.enums.MeterType meterType, LocalDate date) {
        return fixedChargeRepository.findFirstByMeterTypeAndActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToIsNullOrderByVersionDesc(meterType, date)
                .or(() -> fixedChargeRepository.findFirstByMeterTypeAndActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqualOrderByVersionDesc(meterType, date, date))
                .orElse(null);
    }

    private TaxConfig resolveTax(LocalDate date) {
        return taxConfigRepository.findFirstByActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToIsNullOrderByIdDesc(date)
                .or(() -> taxConfigRepository.findFirstByActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqualOrderByIdDesc(date, date))
                .orElse(null);
    }

    private PenaltyConfig resolvePenalty(LocalDate date) {
        return penaltyConfigRepository.findFirstByActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToIsNullOrderByIdDesc(date)
                .or(() -> penaltyConfigRepository.findFirstByActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqualOrderByIdDesc(date, date))
                .orElse(null);
    }

    private Bill getBill(Long id) {
        return billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found"));
    }

    private String billGeneratedMessage(Bill bill) {
        // Exact format required by the exam spec (numeric Month/Year).
        return "Dear %s,%nYour %d/%d utility bill of %s FRW has been successfully processed."
                .formatted(
                        bill.getCustomer().getFullName(),
                        bill.getBillingMonth(),
                        bill.getBillingYear(),
                        bill.getTotalAmount().setScale(2, RoundingMode.HALF_UP)
                );
    }

}
