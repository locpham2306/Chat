package com.chat.common.dto.response;

// Chứa tên field và message của một lỗi validation để trả lên FE.
public record FieldErrorResponse(String field, String message) {
}
