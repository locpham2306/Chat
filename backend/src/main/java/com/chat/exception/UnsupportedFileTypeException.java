package com.chat.exception;

import org.springframework.http.HttpStatus;

public class UnsupportedFileTypeException extends BusinessException {
    public UnsupportedFileTypeException(String messageKey, Object... arguments) {
        super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, messageKey, arguments);
    }
}
