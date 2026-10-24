package com.chat.utils;

import com.chat.common.dto.response.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.function.Function;

public final class PageableUtils {
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private PageableUtils() {
    }

    public static Pageable of(Integer page, Integer size) {
        return of(page, size, Sort.unsorted());
    }

    public static Pageable of(Integer page, Integer size, Sort sort) {
        int resolvedPage = page == null || page < 0 ? DEFAULT_PAGE : page;
        int resolvedSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(resolvedPage, resolvedSize, sort);
    }

    public static <E, R> PageResponse<List<R>> toResponse(Page<E> page, Function<E, R> mapper) {
        var data = page.getContent().stream().map(mapper).toList();
        return PageResponse.of(data, page.getTotalPages(), page.getTotalElements());
    }
}
