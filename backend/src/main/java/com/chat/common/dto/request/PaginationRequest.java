package com.chat.common.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PaginationRequest(
        @Min(value = 0, message = "{validation.min}") Integer page,
        @Min(value = 1, message = "{validation.min}")
        @Max(value = 100, message = "{validation.max}") Integer size) {

    public PaginationRequest {
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
    }
}
