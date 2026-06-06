package com.miguel.app.system.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PagedResponse<T> {
    private boolean success;
    private String message;
    private List<T> data;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
}
