package com.chat.modules.auth.dto.request;

// Nhận session token trong body khi FE yêu cầu đăng xuất mà không dùng token trong cookie.
public record LogoutRequest(String token) {
}
