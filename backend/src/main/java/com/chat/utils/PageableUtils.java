package com.chat.utils;

import com.chat.common.dto.response.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.function.Function;

// Xử lý phân trang dùng chung cho các API trả danh sách.
public final class PageableUtils {
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    // Class chỉ dùng hàm static nên không cho new.
    private PageableUtils() {
    }

    // Tạo phân trang không sắp xếp.
    public static Pageable of(Integer page, Integer size) {
        return of(page, size, Sort.unsorted());
    }

    // Tạo phân trang có sắp xếp, đồng thời chặn size vượt quá 100.
    public static Pageable of(Integer page, Integer size, Sort sort) {
        int resolvedPage = page == null || page < 0 ? DEFAULT_PAGE : page;
        int resolvedSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(resolvedPage, resolvedSize, sort);
    }

    // Đổi danh sách entity sang DTO rồi đóng gói cùng thông tin tổng số trang/phần tử.
    public static <E, R> PageResponse<List<R>> toResponse(Page<E> page, Function<E, R> mapper) {
        List<R> data = page.getContent().stream().map(mapper).toList();
        return PageResponse.of(data, page.getTotalPages(), page.getTotalElements());
    }
}
