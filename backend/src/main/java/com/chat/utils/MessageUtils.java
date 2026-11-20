package com.chat.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

// Lấy message tiếng Việt/Anh theo ngôn ngữ hiện tại của request.
@Component
@RequiredArgsConstructor
public class MessageUtils {
    private final MessageSource messageSource;

    // arguments dùng để thay vào {0}, {1}...; không có key thì trả lại chính key đó.
    public String getMessage(String key, Object... arguments) {
        return messageSource.getMessage(key, arguments, key, LocaleContextHolder.getLocale());
    }
}
