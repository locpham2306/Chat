package com.chat.common.dto.response;

import java.time.Instant;
import java.util.List;

// Chứa thông tin chi tiết của một lỗi HTTP để BE trả lên FE.
public record ErrorDetailResponse(
        Instant timestamp,
        int status,
        String path,
        List<FieldErrorResponse> fieldErrors
) {
    public ErrorDetailResponse {
        fieldErrors = List.copyOf(fieldErrors);
    }
}
