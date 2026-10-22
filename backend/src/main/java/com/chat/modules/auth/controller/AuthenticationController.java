package com.chat.modules.auth.controller;

import com.chat.modules.auth.dto.request.LoginRequest;
import com.chat.modules.auth.dto.request.LogoutRequest;
import com.chat.modules.auth.dto.request.RegisterRequest;
import com.chat.modules.auth.dto.response.LoginResponse;
import com.chat.modules.auth.service.AuthenticationService;
import com.chat.modules.auth.service.SessionCookieService;
import com.chat.modules.auth.service.SessionService;
import com.chat.modules.user.dto.response.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authentication;
    private final SessionService sessions;
    private final SessionCookieService cookies;
    private final MessageSource messages;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authentication.register(request));
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        var session = authentication.login(request);
        cookies.write(response, session);
        return new LoginResponse(messages.getMessage(
                "auth.login.success", null, LocaleContextHolder.getLocale()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestBody(required = false) LogoutRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        var token = cookies.resolveLogoutToken(request, body == null ? null : body.token());
        sessions.revoke(token);
        cookies.clear(response);
        return ResponseEntity.noContent().build();
    }
}
