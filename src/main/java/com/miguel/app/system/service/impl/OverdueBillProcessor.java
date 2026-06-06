package com.miguel.app.system.service.impl;

import com.miguel.app.system.dto.response.BillResponse;
import com.miguel.app.system.entity.Bill;
import com.miguel.app.system.entity.PenaltyConfig;
import com.miguel.app.system.enums.BillStatus;
import com.miguel.app.system.enums.NotificationType;
import com.miguel.app.system.enums.PenaltyType;
import com.miguel.app.system.exception.ResourceNotFoundException;
import com.miguel.app.system.repository.BillRepository;
import com.miguel.app.system.service.interfaces.NotificationService;
import com.miguel.app.system.util.EntityMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies the overdue transition + one-time penalty to a single bill in its own transaction.
 * Running each bill in REQUIRES_NEW means a failure on one bill rolls back only that bill,
 * not the entire nightly batch.
 */
@Component
@RequiredArgsConstructor
public class OverdueBillProcessor {

    private final BillRepository billRepository;
    private final NotificationService notificationService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BillResponse applyOverdue(Long billId, PenaltyConfig penaltyConfig) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found"));

        // Re-check inside the new transaction: another thread may have changed the bill.
        if (bill.getStatus() != BillStatus.APPROVED && bill.getStatus() != BillStatus.PARTIALLY_PAID) {
            return EntityMapper.toBillResponse(bill);
        }

        if (bill.getPenaltyAmount().compareTo(BigDecimal.ZERO) == 0 && penaltyConfig != null) {
            BigDecimal penalty = calculatePenalty(bill, penaltyConfig);
            bill.setPenaltyAmount(penalty);
            bill.setTotalAmount(bill.getTotalAmount().add(penalty));
            bill.setOutstandingBalance(bill.getOutstandingBalance().add(penalty));
        }
        bill.setStatus(BillStatus.OVERDUE);
        Bill saved = billRepository.save(bill);
        notificationService.create(saved.getCustomer(), saved, overdueMessage(saved), NotificationType.BILL_OVERDUE);
        return EntityMapper.toBillResponse(saved);
    }

    private BigDecimal calculatePenalty(Bill bill, PenaltyConfig penaltyConfig) {
        if (penaltyConfig.getPenaltyType() == PenaltyType.FIXED) {
            return penaltyConfig.getAmountOrPercentage().setScale(2, RoundingMode.HALF_UP);
        }
        return bill.getOutstandingBalance()
                .multiply(penaltyConfig.getAmountOrPercentage())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private String overdueMessage(Bill bill) {
        return "Dear %s,%nYour %d/%d utility bill is overdue. Outstanding balance is %s FRW."
                .formatted(
                        bill.getCustomer().getFullName(),
                        bill.getBillingMonth(),
                        bill.getBillingYear(),
                        bill.getOutstandingBalance().setScale(2, RoundingMode.HALF_UP)
                );
    }
}
