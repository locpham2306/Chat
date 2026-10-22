package com.chat.modules.auth.service;

import com.chat.modules.auth.dto.request.LoginRequest;
import com.chat.modules.auth.dto.request.RegisterRequest;
import com.chat.modules.auth.service.IssuedSession;
import com.chat.modules.user.dto.response.UserResponse;

public interface AuthenticationService {
    UserResponse register(RegisterRequest request);

    IssuedSession login(LoginRequest request);
}
