package com.chat.modules.auth.dto.request;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// Nhận session token trong body khi FE yêu cầu đăng xuất mà không dùng token trong cookie.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class LogoutRequest {
    private String token;

    @Override
    public String toString() {
        return "LogoutRequest[token=<redacted>]";
    }
}
