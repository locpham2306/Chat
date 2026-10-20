package com.chat.common.exception;

import org.springframework.http.HttpStatus;

import java.util.Objects;

/**
 * messageKey refers to a public, safe message in the resource bundle.
 * Never pass passwords, tokens or database exception messages as arguments.
 */
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
}
