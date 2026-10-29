package com.chat.modules.auth.controller;

import com.chat.common.wrapper.BaseResponse;
import com.chat.modules.auth.dto.request.LoginRequest;
import com.chat.modules.auth.dto.request.LogoutRequest;
import com.chat.modules.auth.dto.request.RegisterRequest;
import com.chat.modules.auth.dto.response.LoginResponse;
import com.chat.modules.auth.service.AuthenticationService;
import com.chat.modules.auth.service.SessionCookieService;
import com.chat.modules.auth.service.SessionService;
import com.chat.modules.user.dto.response.UserResponse;
import com.chat.modules.websocket.service.WebSocketSessionRegistry;
import com.chat.utils.MessageUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    private final MessageUtils messages;
    private final WebSocketSessionRegistry webSockets;

    @PostMapping("/register")
    public ResponseEntity<BaseResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.created(authentication.register(request)));
    }

    @PostMapping("/login")
    public BaseResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        var session = authentication.login(request);
        cookies.write(response, session);
        return BaseResponse.success(new LoginResponse(messages.getMessage("auth.login.success")));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestBody(required = false) LogoutRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        var token = cookies.resolveLogoutToken(request, body == null ? null : body.token());
        var activeSession = sessions.findActive(token);
        sessions.revoke(token);
        activeSession.ifPresent(identity -> webSockets.closeAppSession(identity.sessionId()));
        cookies.clear(response);
        return ResponseEntity.noContent().build();
    }
}
