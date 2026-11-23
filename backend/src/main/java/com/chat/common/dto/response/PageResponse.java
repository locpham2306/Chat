package com.chat.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Bọc danh sách kết quả cùng tổng số trang và tổng số phần tử để trả lên FE.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private T data;
    private long totalPages;
    private long totalElements;

    public static <T> PageResponse<T> of(T data, long totalPages, long totalElements) {
        return new PageResponse<>(data, totalPages, totalElements);
    }
}
