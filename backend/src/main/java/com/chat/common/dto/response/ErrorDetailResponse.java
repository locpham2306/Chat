package com.chat.common.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

// Chứa thông tin chi tiết của một lỗi HTTP để BE trả lên FE.
@Getter
@Setter
@NoArgsConstructor
public class ErrorDetailResponse {
    private Instant timestamp;
    private int status;
    private String path;
    private List<FieldErrorResponse> fieldErrors;

    public ErrorDetailResponse(
            Instant timestamp,
            int status,
            String path,
            List<FieldErrorResponse> fieldErrors) {
        this.timestamp = timestamp;
        this.status = status;
        this.path = path;
        this.fieldErrors = List.copyOf(fieldErrors);
    }
}
