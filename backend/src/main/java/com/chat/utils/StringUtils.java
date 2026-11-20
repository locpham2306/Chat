package com.chat.utils;

import java.util.Locale;

// Các hàm xử lý chuỗi dùng chung.
public final class StringUtils {

    // Class chỉ dùng hàm static nên không cho new.
    private StringUtils() {
    }

    // Xóa khoảng trắng đầu/cuối, value null thì vẫn trả null.
    public static String trim(String value) {
        return value == null ? null : value.trim();
    }

    // Kiểm tra Content-Type có phải ảnh không, không phân biệt hoa thường.
    public static boolean isImageContentType(String contentType) {
        return contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("image/");
    }
}
