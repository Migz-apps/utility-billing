package com.miguel.app.system.service.interfaces;

import com.miguel.app.system.dto.request.MeterRequest;
import com.miguel.app.system.dto.response.MeterResponse;
import com.miguel.app.system.dto.response.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface MeterService {
    MeterResponse create(MeterRequest request);
    PagedResponse<MeterResponse> getAll(Pageable pageable);
    PagedResponse<MeterResponse> getActive(Pageable pageable);
    MeterResponse getById(Long id);
    PagedResponse<MeterResponse> getByCustomer(Long customerId, Pageable pageable);
    PagedResponse<MeterResponse> getMyMeters(Pageable pageable);
    MeterResponse update(Long id, MeterRequest request);
    MeterResponse activate(Long id);
    MeterResponse deactivate(Long id);
}
