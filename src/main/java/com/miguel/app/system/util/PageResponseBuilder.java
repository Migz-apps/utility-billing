package com.miguel.app.system.util;

import com.miguel.app.system.dto.response.PagedResponse;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

public final class PageResponseBuilder {

    private PageResponseBuilder() {
    }

    public static <T, R> PagedResponse<R> build(Page<T> page, String message, Function<T, R> mapper) {
        List<R> data = page.getContent().stream().map(mapper).toList();
        return PagedResponse.<R>builder()
                .success(true)
                .message(message)
                .data(data)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .page(page.getNumber())
                .size(page.getSize())
                .build();
    }
}
