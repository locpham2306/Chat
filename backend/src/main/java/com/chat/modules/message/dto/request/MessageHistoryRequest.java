package com.chat.modules.message.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.UUID;

public record MessageHistoryRequest(
        UUID before,
        @Min(value = 1, message = "{validation.min}")
        @Max(value = 100, message = "{validation.max}") Integer limit) {

    public MessageHistoryRequest {
        limit = limit == null ? 30 : limit;
    }
}
