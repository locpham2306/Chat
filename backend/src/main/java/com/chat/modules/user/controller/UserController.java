package com.chat.modules.user.controller;

import com.chat.common.dto.response.PageResponse;
import com.chat.common.wrapper.BaseResponse;
import com.chat.modules.auth.security.SessionAuthenticationFilter;
import com.chat.modules.auth.service.SessionIdentity;
import com.chat.modules.user.dto.request.UpdateProfileRequest;
import com.chat.modules.user.dto.response.UserResponse;
import com.chat.modules.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService users;

    @GetMapping
    public BaseResponse<PageResponse<List<UserResponse>>> searchUsers(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "{validation.min}") Integer page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "{validation.min}")
            @Max(value = 100, message = "{validation.max}") Integer size) {
        return BaseResponse.success(users.searchUsers(session.userId(), keyword, page, size));
    }

    @GetMapping("/me")
    public BaseResponse<UserResponse> getCurrentUser(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session) {
        return BaseResponse.success(users.getCurrentUser(session.userId()));
    }

    @PutMapping("/me")
    public BaseResponse<UserResponse> updateCurrentUser(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @Valid @RequestBody UpdateProfileRequest request) {
        return BaseResponse.success(users.updateCurrentUser(session.userId(), request));
    }
}
