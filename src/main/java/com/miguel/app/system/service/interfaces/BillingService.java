package com.miguel.app.system.service.interfaces;

import com.miguel.app.system.dto.response.BillResponse;
import com.miguel.app.system.dto.response.PagedResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface BillingService {
    BillResponse generateBillForReadingId(Long readingId);
    List<BillResponse> generateMonthly(Integer month, Integer year);
    PagedResponse<BillResponse> getAll(Pageable pageable);
    BillResponse getById(Long id);
    BillResponse getByReference(String billReference);
    PagedResponse<BillResponse> getByCustomer(Long customerId, Pageable pageable);
    PagedResponse<BillResponse> getMyBills(Pageable pageable);
    BillResponse approve(Long id);
    BillResponse cancel(Long id);
    List<BillResponse> processOverdueBills();
}
