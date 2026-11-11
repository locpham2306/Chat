package com.chat.modules.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface SessionCookieService {
    void write(HttpServletResponse response, IssuedSession session);

    void clear(HttpServletResponse response);

    String read(HttpServletRequest request);

    String resolveLogoutToken(HttpServletRequest request, String bodyToken);
}
