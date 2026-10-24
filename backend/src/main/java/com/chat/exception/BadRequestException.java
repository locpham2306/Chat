package com.chat.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends BusinessException {
    public BadRequestException(String messageKey, Object... arguments) {
        super(HttpStatus.BAD_REQUEST, messageKey, arguments);
    }
}
