package com.chat.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Chứa tên field và message của một lỗi validation để trả lên FE.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FieldErrorResponse {
    private String field;
    private String message;
}
