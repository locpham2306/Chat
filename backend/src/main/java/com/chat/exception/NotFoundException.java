package com.chat.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BusinessException {
    public NotFoundException(String messageKey, Object... arguments) {
        super(HttpStatus.NOT_FOUND, messageKey, arguments);
    }
}
