package com.miguel.app.system.service.interfaces;

import com.miguel.app.system.dto.request.PaymentRequest;
import com.miguel.app.system.dto.response.PagedResponse;
import com.miguel.app.system.dto.response.PaymentResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface PaymentService {
    PaymentResponse create(PaymentRequest request);
    PagedResponse<PaymentResponse> getAll(Pageable pageable);
    PaymentResponse getById(Long id);
    List<PaymentResponse> getByBill(Long billId);
    PagedResponse<PaymentResponse> getByCustomer(Long customerId, Pageable pageable);
    PagedResponse<PaymentResponse> getMyPayments(Pageable pageable);
}
