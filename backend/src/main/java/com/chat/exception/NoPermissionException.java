package com.chat.exception;

import org.springframework.http.HttpStatus;

public class NoPermissionException extends BusinessException {
    public NoPermissionException(String messageKey, Object... arguments) {
        super(HttpStatus.FORBIDDEN, messageKey, arguments);
    }
}
