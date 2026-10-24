package com.chat.modules.user.controller;

import com.chat.modules.auth.security.SessionAuthenticationFilter;
import com.chat.modules.auth.service.SessionIdentity;
import com.chat.modules.user.dto.response.UserResponse;
import com.chat.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService users;

    @GetMapping("/me")
    public UserResponse getCurrentUser(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session) {
        return users.getCurrentUser(session.userId());
    }
}
