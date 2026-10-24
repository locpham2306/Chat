package com.chat.common.wrapper;

import com.chat.common.constant.StatusCode;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BaseResponse<T>(String code, String strCode, String message, T content) {

    public static <T> BaseResponse<T> success(T content) {
        return success(content, "SUCCESSFUL");
    }

    public static <T> BaseResponse<T> success(T content, String message) {
        return new BaseResponse<>(String.valueOf(StatusCode.SUCCESSFUL.getCode()),
                "success", message, content);
    }

    public static <T> BaseResponse<T> created(T content) {
        return success(content);
    }

    public static <T> BaseResponse<T> error(
            StatusCode statusCode, String strCode, String message, T content) {
        return new BaseResponse<>(String.valueOf(statusCode.getCode()), strCode, message, content);
    }
}
