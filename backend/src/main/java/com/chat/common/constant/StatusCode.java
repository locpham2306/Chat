package com.chat.common.constant;

import org.springframework.http.HttpStatus;

public enum StatusCode {
    SUCCESSFUL(0, HttpStatus.OK),
    INVALID_INPUT_DATA(3, HttpStatus.BAD_REQUEST),
    AUTHENTICATION_FAILED(8, HttpStatus.UNAUTHORIZED),
    AUTHORIZATION_FAILED(9, HttpStatus.FORBIDDEN),
    ERROR_IN_BACKEND(10, HttpStatus.INTERNAL_SERVER_ERROR),
    NOT_EXISTED(11, HttpStatus.NOT_FOUND),
    EXISTED(13, HttpStatus.CONFLICT),
    PAYLOAD_TOO_LARGE(18, HttpStatus.PAYLOAD_TOO_LARGE),
    UNSUPPORTED_MEDIA_TYPE(19, HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    private final int code;
    private final HttpStatus httpStatus;

    StatusCode(int code, HttpStatus httpStatus) {
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public static StatusCode from(HttpStatus status) {
        return switch (status) {
            case UNAUTHORIZED -> AUTHENTICATION_FAILED;
            case FORBIDDEN -> AUTHORIZATION_FAILED;
            case NOT_FOUND -> NOT_EXISTED;
            case CONFLICT -> EXISTED;
            case PAYLOAD_TOO_LARGE -> PAYLOAD_TOO_LARGE;
            case UNSUPPORTED_MEDIA_TYPE -> UNSUPPORTED_MEDIA_TYPE;
            default -> status.is5xxServerError() ? ERROR_IN_BACKEND : INVALID_INPUT_DATA;
        };
    }
}
