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
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public BaseResponse<PageResponse<List<UserResponse>>> searchUsers(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @Valid @ModelAttribute SearchRequest request) {
        UUID currentUserId = session.userId();
        PageResponse<List<UserResponse>> users = userService.searchUsers(
                currentUserId, request.keyword(), request.page(), request.size());
        return BaseResponse.success(users);
    }

    @GetMapping("/me")
    public BaseResponse<UserResponse> getCurrentUser(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session) {
        UUID currentUserId = session.userId();
        UserResponse user = userService.getCurrentUser(currentUserId);
        return BaseResponse.success(user);
    }

    @PutMapping("/me")
    public BaseResponse<UserResponse> updateCurrentUser(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @Valid @RequestBody UpdateProfileRequest request) {
        UUID currentUserId = session.userId();
        UserResponse user = userService.updateCurrentUser(currentUserId, request);
        return BaseResponse.success(user);
    }
}
