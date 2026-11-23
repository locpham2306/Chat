package com.chat.common.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// Nhận page và size từ FE cho các API phân trang thông thường.
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class PaginationRequest {
    @Min(value = 0, message = "{validation.min}")
    private Integer page;

    @Min(value = 1, message = "{validation.min}")
    @Max(value = 100, message = "{validation.max}")
    private Integer size;

    public PaginationRequest() {
        this(null, null);
    }

    public PaginationRequest(Integer page, Integer size) {
        setPage(page);
        setSize(size);
    }

    public void setPage(Integer page) {
        this.page = page == null ? 0 : page;
    }

    public void setSize(Integer size) {
        this.size = size == null ? 20 : size;
    }
}
