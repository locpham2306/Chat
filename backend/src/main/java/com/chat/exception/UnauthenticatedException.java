package com.chat.exception;

import org.springframework.http.HttpStatus;

public class UnauthenticatedException extends BusinessException {
    public UnauthenticatedException(String messageKey, Object... arguments) {
        super(HttpStatus.UNAUTHORIZED, messageKey, arguments);
    }
}
