package com.miguel.app.system.config;

import com.miguel.app.system.entity.Customer;
import com.miguel.app.system.entity.FixedCharge;
import com.miguel.app.system.entity.Meter;
import com.miguel.app.system.entity.PenaltyConfig;
import com.miguel.app.system.entity.Tariff;
import com.miguel.app.system.entity.TaxConfig;
import com.miguel.app.system.entity.User;
import com.miguel.app.system.enums.CustomerStatus;
import com.miguel.app.system.enums.MeterStatus;
import com.miguel.app.system.enums.MeterType;
import com.miguel.app.system.enums.PenaltyType;
import com.miguel.app.system.enums.Role;
import com.miguel.app.system.enums.TariffType;
import com.miguel.app.system.enums.UserStatus;
import com.miguel.app.system.repository.CustomerRepository;
import com.miguel.app.system.repository.FixedChargeRepository;
import com.miguel.app.system.repository.MeterRepository;
import com.miguel.app.system.repository.PenaltyConfigRepository;
import com.miguel.app.system.repository.TariffRepository;
import com.miguel.app.system.repository.TaxConfigRepository;
import com.miguel.app.system.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final MeterRepository meterRepository;
    private final TariffRepository tariffRepository;
    private final FixedChargeRepository fixedChargeRepository;
    private final TaxConfigRepository taxConfigRepository;
    private final PenaltyConfigRepository penaltyConfigRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Override
    public void run(String... args) {
        backfillExistingVerifiedUsers();

        if (!seedEnabled || userRepository.count() > 0) {
            return;
        }

        User admin = createUser("System Admin", "admin@utility.rw", "0780000001", "Admin123!", Role.ROLE_ADMIN);
        createUser("Meter Operator", "operator@utility.rw", "0780000002", "Operator123!", Role.ROLE_OPERATOR);
        createUser("Finance Officer", "finance@utility.rw", "0780000003", "Finance123!", Role.ROLE_FINANCE);
        User customerUser = createUser("Default Customer", "customer@utility.rw", "0780000004", "Customer123!", Role.ROLE_CUSTOMER);

        Customer customer = new Customer();
        customer.setFullName("Default Customer");
        customer.setNationalId("1234567890123456");
        customer.setEmail("customer@utility.rw");
        customer.setPhoneNumber("0780000004");
        customer.setAddress("Kigali, Rwanda");
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setUser(customerUser);
        Customer savedCustomer = customerRepository.save(customer);

        meterRepository.save(createMeter("WATER-0001", MeterType.WATER, savedCustomer));
        meterRepository.save(createMeter("ELEC-0001", MeterType.ELECTRICITY, savedCustomer));

        tariffRepository.save(createTariff("Water Flat Tariff", MeterType.WATER, new BigDecimal("300"), 1));
        tariffRepository.save(createTariff("Electricity Flat Tariff", MeterType.ELECTRICITY, new BigDecimal("500"), 1));

        fixedChargeRepository.save(createFixedCharge(MeterType.WATER, new BigDecimal("1000")));
        fixedChargeRepository.save(createFixedCharge(MeterType.ELECTRICITY, new BigDecimal("1500")));

        TaxConfig taxConfig = new TaxConfig();
        taxConfig.setName("VAT");
        taxConfig.setPercentage(new BigDecimal("18"));
        taxConfig.setActive(true);
        taxConfig.setEffectiveFrom(LocalDate.now().minusYears(1));
        taxConfigRepository.save(taxConfig);

        PenaltyConfig penaltyConfig = new PenaltyConfig();
        penaltyConfig.setName("Late Payment Penalty");
        penaltyConfig.setPenaltyType(PenaltyType.PERCENTAGE);
        penaltyConfig.setAmountOrPercentage(new BigDecimal("5"));
        penaltyConfig.setGracePeriodDays(5);
        penaltyConfig.setActive(true);
        penaltyConfig.setEffectiveFrom(LocalDate.now().minusYears(1));
        penaltyConfigRepository.save(penaltyConfig);
    }

    private User createUser(String fullName, String email, String phoneNumber, String rawPassword, Role role) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private void backfillExistingVerifiedUsers() {
        userRepository.findAll().stream()
                .filter(user -> !user.isEmailVerified())
                .filter(user -> user.getRole() != Role.ROLE_CUSTOMER
                        || user.getCustomer() != null
                        || user.getStatus() == UserStatus.ACTIVE)
                .forEach(user -> {
                    user.setEmailVerified(true);
                    userRepository.save(user);
                });
    }

    private Meter createMeter(String number, MeterType meterType, Customer customer) {
        Meter meter = new Meter();
        meter.setMeterNumber(number);
        meter.setMeterType(meterType);
        meter.setInstallationDate(LocalDate.now().minusMonths(6));
        meter.setStatus(MeterStatus.ACTIVE);
        meter.setCustomer(customer);
        return meter;
    }

    private Tariff createTariff(String name, MeterType meterType, BigDecimal rate, int version) {
        Tariff tariff = new Tariff();
        tariff.setName(name);
        tariff.setMeterType(meterType);
        tariff.setTariffType(TariffType.FLAT);
        tariff.setRatePerUnit(rate);
        tariff.setVersion(version);
        tariff.setEffectiveFrom(LocalDate.now().minusYears(1));
        tariff.setActive(true);
        return tariff;
    }

    private FixedCharge createFixedCharge(MeterType meterType, BigDecimal amount) {
        FixedCharge fixedCharge = new FixedCharge();
        fixedCharge.setMeterType(meterType);
        fixedCharge.setAmount(amount);
        fixedCharge.setVersion(1);
        fixedCharge.setEffectiveFrom(LocalDate.now().minusYears(1));
        fixedCharge.setActive(true);
        return fixedCharge;
    }
}
