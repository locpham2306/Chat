package com.chat.modules.auth.controller;

import com.chat.common.wrapper.BaseResponse;
import com.chat.modules.auth.dto.request.LoginRequest;
import com.chat.modules.auth.dto.request.LogoutRequest;
import com.chat.modules.auth.dto.request.RegisterRequest;
import com.chat.modules.auth.dto.response.LoginResponse;
import com.chat.modules.auth.service.AuthenticationService;
import com.chat.modules.auth.service.IssuedSession;
import com.chat.modules.auth.service.SessionCookieService;
import com.chat.modules.auth.service.SessionIdentity;
import com.chat.modules.auth.service.SessionService;
import com.chat.modules.user.dto.response.UserResponse;
import com.chat.integration.websocket.service.WebSocketSessionRegistry;
import com.chat.utils.MessageUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final SessionService sessionService;
    private final SessionCookieService sessionCookieService;
    private final MessageUtils messageUtils;
    private final WebSocketSessionRegistry webSocketSessionRegistry;

    @PostMapping("/register")
    public ResponseEntity<BaseResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse user = authenticationService.register(request);
        BaseResponse<UserResponse> response = BaseResponse.created(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public BaseResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        IssuedSession session = authenticationService.login(request);
        sessionCookieService.write(response, session);
        String message = messageUtils.getMessage("auth.login.success");
        LoginResponse loginResponse = new LoginResponse(message);
        return BaseResponse.success(loginResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestBody(required = false) LogoutRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        String bodyToken = null;
        if (body != null) {
            bodyToken = body.getToken();
        }

        String token = sessionCookieService.resolveLogoutToken(request, bodyToken);
        Optional<SessionIdentity> activeSession = sessionService.findActive(token);
        sessionService.revoke(token);
        if (activeSession.isPresent()) {
            SessionIdentity identity = activeSession.get();
            webSocketSessionRegistry.closeAppSession(identity.sessionId());
        }
        sessionCookieService.clear(response);
        return ResponseEntity.noContent().build();
    }
}
