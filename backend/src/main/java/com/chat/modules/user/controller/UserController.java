package com.chat.modules.user.controller;

import com.chat.common.dto.response.PageResponse;
import com.chat.common.wrapper.BaseResponse;
import com.chat.modules.auth.security.SessionAuthenticationFilter;
import com.chat.modules.auth.service.SessionIdentity;
import com.chat.modules.user.dto.request.SearchRequest;
import com.chat.modules.user.dto.request.UpdateProfileRequest;
import com.chat.modules.user.dto.response.UserResponse;
import com.chat.modules.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public BaseResponse<PageResponse<List<UserResponse>>> searchUsers(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @Valid @ModelAttribute SearchRequest request) {
        return BaseResponse.success(userService.searchUsers(session.userId(), request.keyword(), request.page(), request.size()));
    }

    @GetMapping("/me")
    public BaseResponse<UserResponse> getCurrentUser(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session) {
        return BaseResponse.success(userService.getCurrentUser(session.userId()));
    }

    @PutMapping("/me")
    public BaseResponse<UserResponse> updateCurrentUser(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @Valid @RequestBody UpdateProfileRequest request) {
        return BaseResponse.success(userService.updateCurrentUser(session.userId(), request));
    }
}
