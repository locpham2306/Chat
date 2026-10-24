package com.chat.exception;

import org.springframework.http.HttpStatus;

public class DataExistedException extends BusinessException {
    public DataExistedException(String messageKey, Object... arguments) {
        super(HttpStatus.CONFLICT, messageKey, arguments);
    }
}
