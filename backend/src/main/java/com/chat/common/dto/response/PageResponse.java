package com.chat.common.dto.response;

public record PageResponse<T>(T data, long totalPages, long totalElements) {

    public static <T> PageResponse<T> of(T data, long totalPages, long totalElements) {
        return new PageResponse<>(data, totalPages, totalElements);
    }
}
