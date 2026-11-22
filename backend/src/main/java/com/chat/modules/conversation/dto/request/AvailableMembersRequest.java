package com.chat.modules.conversation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

// Nhận điều kiện tìm kiếm và phân trang khi FE lấy các user có thể thêm vào nhóm.
public record AvailableMembersRequest(
        String keyword,
        @Min(value = 0, message = "{validation.min}") Integer page,
        @Min(value = 1, message = "{validation.min}")
        @Max(value = 100, message = "{validation.max}") Integer size) {

    public AvailableMembersRequest {
        keyword = keyword == null ? "" : keyword;
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
    }
}
