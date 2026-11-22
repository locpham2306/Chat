package com.chat.common.dto.response;

// Bọc danh sách kết quả cùng tổng số trang và tổng số phần tử để trả lên FE.
public record PageResponse<T>(T data, long totalPages, long totalElements) {

    public static <T> PageResponse<T> of(T data, long totalPages, long totalElements) {
        return new PageResponse<>(data, totalPages, totalElements);
    }
}
