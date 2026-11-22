package com.chat.modules.user.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

// Nhận từ khóa và phân trang khi FE tìm kiếm user.
public record SearchRequest(
        String keyword,
        @Min(value = 0, message = "{validation.min}") Integer page,
        @Min(value = 1, message = "{validation.min}")
        @Max(value = 100, message = "{validation.max}") Integer size) {

    public SearchRequest {
        keyword = keyword == null ? "" : keyword;
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
    }
}
