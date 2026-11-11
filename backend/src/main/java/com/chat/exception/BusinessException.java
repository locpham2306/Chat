package com.chat.exception;

import org.springframework.http.HttpStatus;

import java.util.Objects;

public class BusinessException extends RuntimeException {
    private final HttpStatus status;
    private final String messageKey;
    private final Object[] arguments;

    public BusinessException(HttpStatus status, String messageKey, Object... arguments) {
        super(messageKey);
        this.status = Objects.requireNonNull(status);
        if (!status.is4xxClientError()) {
            throw new IllegalArgumentException("Business exceptions must use a 4xx status");
        }
        this.messageKey = Objects.requireNonNull(messageKey);
        this.arguments = arguments == null ? new Object[0] : arguments.clone();
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getArguments() {
        return arguments.clone();
    }

    public static BusinessException badRequest(String messageKey, Object... arguments) {
        return new BusinessException(HttpStatus.BAD_REQUEST, messageKey, arguments);
    }

    public static BusinessException unauthenticated(String messageKey, Object... arguments) {
        return new BusinessException(HttpStatus.UNAUTHORIZED, messageKey, arguments);
    }

    public static BusinessException forbidden(String messageKey, Object... arguments) {
        return new BusinessException(HttpStatus.FORBIDDEN, messageKey, arguments);
    }

    public static BusinessException notFound(String messageKey, Object... arguments) {
        return new BusinessException(HttpStatus.NOT_FOUND, messageKey, arguments);
    }

    public static BusinessException conflict(String messageKey, Object... arguments) {
        return new BusinessException(HttpStatus.CONFLICT, messageKey, arguments);
    }

    public static BusinessException payloadTooLarge(String messageKey, Object... arguments) {
        return new BusinessException(HttpStatus.PAYLOAD_TOO_LARGE, messageKey, arguments);
    }
}
