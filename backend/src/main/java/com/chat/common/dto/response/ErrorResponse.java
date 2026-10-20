package com.chat.common.dto.response;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldErrorResponse> fieldErrors
) {
    public ErrorResponse {
        fieldErrors = List.copyOf(fieldErrors);
    }
}
