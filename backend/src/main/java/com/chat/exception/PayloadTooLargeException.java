package com.chat.exception;

import org.springframework.http.HttpStatus;

public class PayloadTooLargeException extends BusinessException {
    public PayloadTooLargeException(String messageKey, Object... arguments) {
        super(HttpStatus.PAYLOAD_TOO_LARGE, messageKey, arguments);
    }
}
