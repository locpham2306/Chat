package com.chat.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

// Cấu hình message validation và ngôn ngữ trả về cho FE.
@Configuration
public class ValidationConfig {

    // Cho @Valid lấy message từ messages.properties/messages_en.properties.
    @Bean
    public LocalValidatorFactoryBean validator(MessageSource messageSource) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setValidationMessageSource(messageSource);
        return validator;
    }

    // Đọc Accept-Language để chọn tiếng Việt/Anh, mặc định là tiếng Việt.
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.forLanguageTag("vi"));
        resolver.setSupportedLocales(List.of(Locale.forLanguageTag("vi"), Locale.ENGLISH));
        return resolver;
    }
}
