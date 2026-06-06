package com.miguel.app.system.util;

import com.miguel.app.system.dto.response.BillResponse;
import com.miguel.app.system.dto.response.CustomerResponse;
import com.miguel.app.system.dto.response.FixedChargeResponse;
import com.miguel.app.system.dto.response.MeterReadingResponse;
import com.miguel.app.system.dto.response.MeterResponse;
import com.miguel.app.system.dto.response.NotificationResponse;
import com.miguel.app.system.dto.response.PaymentResponse;
import com.miguel.app.system.dto.response.PenaltyConfigResponse;
import com.miguel.app.system.dto.response.TariffResponse;
import com.miguel.app.system.dto.response.TariffTierResponse;
import com.miguel.app.system.dto.response.TaxConfigResponse;
import com.miguel.app.system.dto.response.UserResponse;
import com.miguel.app.system.entity.Bill;
import com.miguel.app.system.entity.Customer;
import com.miguel.app.system.entity.FixedCharge;
import com.miguel.app.system.entity.Meter;
import com.miguel.app.system.entity.MeterReading;
import com.miguel.app.system.entity.Notification;
import com.miguel.app.system.entity.Payment;
import com.miguel.app.system.entity.PenaltyConfig;
import com.miguel.app.system.entity.Tariff;
import com.miguel.app.system.entity.TariffTier;
import com.miguel.app.system.entity.TaxConfig;
import com.miguel.app.system.entity.User;
import java.util.List;

public final class EntityMapper {

    private EntityMapper() {
    }

    public static UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole(),
                user.isEmailVerified(),
                user.getStatus(),
                user.getCustomer() != null ? user.getCustomer().getId() : null,
                user.getCustomer() != null ? user.getCustomer().getStatus() : null,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public static CustomerResponse toCustomerResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFullName(),
                customer.getNationalId(),
                customer.getEmail(),
                customer.getPhoneNumber(),
                customer.getAddress(),
                customer.getStatus(),
                customer.getUser() != null ? customer.getUser().getId() : null,
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }

    public static MeterResponse toMeterResponse(Meter meter) {
        return new MeterResponse(
                meter.getId(),
                meter.getMeterNumber(),
                meter.getMeterType(),
                meter.getInstallationDate(),
                meter.getStatus(),
                meter.getCustomer().getId(),
                meter.getCustomer().getFullName(),
                meter.getCreatedAt(),
                meter.getUpdatedAt()
        );
    }

    public static MeterReadingResponse toMeterReadingResponse(MeterReading reading) {
        return new MeterReadingResponse(
                reading.getId(),
                reading.getMeter().getId(),
                reading.getMeter().getMeterNumber(),
                reading.getPreviousReading(),
                reading.getCurrentReading(),
                reading.getConsumption(),
                reading.getReadingDate(),
                reading.getBillingMonth(),
                reading.getBillingYear(),
                reading.getCapturedBy().getId(),
                reading.getCapturedBy().getFullName(),
                reading.getBill() != null ? reading.getBill().getBillReference() : null,
                reading.getCreatedAt()
        );
    }

    public static TariffTierResponse toTariffTierResponse(TariffTier tier) {
        return new TariffTierResponse(tier.getId(), tier.getMinUnits(), tier.getMaxUnits(), tier.getRatePerUnit());
    }

    public static TariffResponse toTariffResponse(Tariff tariff) {
        List<TariffTierResponse> tiers = tariff.getTiers().stream().map(EntityMapper::toTariffTierResponse).toList();
        return new TariffResponse(
                tariff.getId(),
                tariff.getName(),
                tariff.getMeterType(),
                tariff.getTariffType(),
                tariff.getRatePerUnit(),
                tariff.getVersion(),
                tariff.getEffectiveFrom(),
                tariff.getEffectiveTo(),
                tariff.isActive(),
                tariff.getCreatedAt(),
                tariff.getUpdatedAt(),
                tiers
        );
    }

    public static FixedChargeResponse toFixedChargeResponse(FixedCharge fixedCharge) {
        return new FixedChargeResponse(
                fixedCharge.getId(),
                fixedCharge.getMeterType(),
                fixedCharge.getAmount(),
                fixedCharge.getVersion(),
                fixedCharge.getEffectiveFrom(),
                fixedCharge.getEffectiveTo(),
                fixedCharge.isActive()
        );
    }

    public static TaxConfigResponse toTaxConfigResponse(TaxConfig taxConfig) {
        return new TaxConfigResponse(
                taxConfig.getId(),
                taxConfig.getName(),
                taxConfig.getPercentage(),
                taxConfig.isActive(),
                taxConfig.getEffectiveFrom(),
                taxConfig.getEffectiveTo()
        );
    }

    public static PenaltyConfigResponse toPenaltyConfigResponse(PenaltyConfig penaltyConfig) {
        return new PenaltyConfigResponse(
                penaltyConfig.getId(),
                penaltyConfig.getName(),
                penaltyConfig.getPenaltyType(),
                penaltyConfig.getAmountOrPercentage(),
                penaltyConfig.getGracePeriodDays(),
                penaltyConfig.isActive(),
                penaltyConfig.getEffectiveFrom(),
                penaltyConfig.getEffectiveTo()
        );
    }

    public static BillResponse toBillResponse(Bill bill) {
        return new BillResponse(
                bill.getId(),
                bill.getBillReference(),
                bill.getCustomer().getId(),
                bill.getCustomer().getFullName(),
                bill.getMeter().getId(),
                bill.getMeter().getMeterNumber(),
                bill.getMeterReading().getId(),
                bill.getBillingMonth(),
                bill.getBillingYear(),
                bill.getConsumption(),
                bill.getTariffAmount(),
                bill.getFixedCharge(),
                bill.getTaxAmount(),
                bill.getPenaltyAmount(),
                bill.getTotalAmount(),
                bill.getAmountPaid(),
                bill.getOutstandingBalance(),
                bill.getStatus(),
                bill.getDueDate(),
                bill.getApprovedBy() != null ? bill.getApprovedBy().getId() : null,
                bill.getApprovedBy() != null ? bill.getApprovedBy().getFullName() : null,
                bill.getApprovedAt(),
                bill.getCreatedAt(),
                bill.getUpdatedAt()
        );
    }

    public static PaymentResponse toPaymentResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getPaymentReference(),
                payment.getBill().getId(),
                payment.getBill().getBillReference(),
                payment.getCustomer().getId(),
                payment.getCustomer().getFullName(),
                payment.getAmountPaid(),
                payment.getPaymentMethod(),
                payment.getPaymentDate(),
                payment.getRecordedBy().getId(),
                payment.getRecordedBy().getFullName(),
                payment.getCreatedAt()
        );
    }

    public static NotificationResponse toNotificationResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getCustomer().getId(),
                notification.getBill() != null ? notification.getBill().getId() : null,
                notification.getBill() != null ? notification.getBill().getBillReference() : null,
                notification.getMessage(),
                notification.getNotificationType(),
                notification.getStatus(),
                notification.getCreatedAt()
        );
    }
}
