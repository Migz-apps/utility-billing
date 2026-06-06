package com.miguel.app.system.service.interfaces;

import com.miguel.app.system.dto.request.AdminConvertUserToCustomerRequest;
import com.miguel.app.system.dto.request.CustomerProfileRequest;
import com.miguel.app.system.dto.request.CustomerRequest;
import com.miguel.app.system.dto.response.CustomerResponse;
import com.miguel.app.system.dto.response.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface CustomerService {
    CustomerResponse create(CustomerRequest request);
    PagedResponse<CustomerResponse> getAll(Pageable pageable);
    CustomerResponse getById(Long id);
    CustomerResponse update(Long id, CustomerRequest request);
    CustomerResponse activate(Long id);
    CustomerResponse deactivate(Long id);
    CustomerResponse delete(Long id);
    CustomerResponse createMyProfile(CustomerProfileRequest request);
    CustomerResponse createForUser(Long userId, AdminConvertUserToCustomerRequest request);
    CustomerResponse getMe();
    PagedResponse<CustomerResponse> getPendingVerification(Pageable pageable);
    CustomerResponse activateByUserId(Long userId);
}
