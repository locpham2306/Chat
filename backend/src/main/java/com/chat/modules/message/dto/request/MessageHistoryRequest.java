package com.chat.modules.message.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

// Nhận cursor và giới hạn số lượng khi FE tải lịch sử tin nhắn.
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class MessageHistoryRequest {
    private UUID before;

    @Min(value = 1, message = "{validation.min}")
    @Max(value = 100, message = "{validation.max}")
    private Integer limit;

    public MessageHistoryRequest() {
        this(null, null);
    }

    public MessageHistoryRequest(UUID before, Integer limit) {
        this.before = before;
        setLimit(limit);
    }

    public void setLimit(Integer limit) {
        this.limit = limit == null ? 30 : limit;
    }
}
