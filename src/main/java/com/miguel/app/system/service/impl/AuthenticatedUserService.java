package com.miguel.app.system.service.impl;

import com.miguel.app.system.entity.Customer;
import com.miguel.app.system.entity.User;
import com.miguel.app.system.enums.CustomerStatus;
import com.miguel.app.system.enums.UserStatus;
import com.miguel.app.system.exception.BusinessRuleException;
import com.miguel.app.system.exception.ResourceNotFoundException;
import com.miguel.app.system.exception.UnauthorizedException;
import com.miguel.app.system.repository.CustomerRepository;
import com.miguel.app.system.repository.UserRepository;
import com.miguel.app.system.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticatedUserService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    public User currentUser() {
        String username = SecurityUtils.currentUsername();
        if (username == null) {
            throw new UnauthorizedException("No authenticated user found");
        }
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user no longer exists"));
    }

    public Customer currentCustomer() {
        User user = currentUser();
        return customerRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("No customer profile is linked to the current user"));
    }

    public Customer currentActiveCustomer() {
        User user = currentUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("Your user account is inactive");
        }

        Customer customer = currentCustomer();
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new BusinessRuleException("Your customer profile is pending admin verification");
        }

        return customer;
    }
}
