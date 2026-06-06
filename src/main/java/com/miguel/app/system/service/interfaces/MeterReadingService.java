package com.miguel.app.system.service.interfaces;

import com.miguel.app.system.dto.request.MeterReadingRequest;
import com.miguel.app.system.dto.response.MeterReadingResponse;
import com.miguel.app.system.dto.response.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface MeterReadingService {
    MeterReadingResponse create(MeterReadingRequest request);
    PagedResponse<MeterReadingResponse> getAll(Pageable pageable);
    MeterReadingResponse getById(Long id);
    PagedResponse<MeterReadingResponse> getByMeter(Long meterId, Pageable pageable);
    PagedResponse<MeterReadingResponse> getMonthly(Integer month, Integer year, Pageable pageable);
}
