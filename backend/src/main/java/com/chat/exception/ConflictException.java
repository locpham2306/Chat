package com.chat.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends BusinessException {
    public ConflictException(String messageKey, Object... arguments) {
        super(HttpStatus.CONFLICT, messageKey, arguments);
    }
}
