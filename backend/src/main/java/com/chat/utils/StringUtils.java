package com.chat.utils;

import java.util.Locale;

public final class StringUtils {
    private StringUtils() {
    }

    public static String trim(String value) {
        return value == null ? null : value.trim();
    }

    public static boolean isImageContentType(String contentType) {
        return contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("image/");
    }
}
