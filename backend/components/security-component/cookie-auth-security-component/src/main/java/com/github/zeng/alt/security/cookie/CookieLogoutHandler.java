package com.github.zeng.alt.security.cookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;

import java.util.Arrays;

/**
 * Cookie 登出处理器.
 * <p>
 * 从 Cookie 中提取 sessionId，从缓存中删除会话，并清除 Cookie。
 *
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年10月07日
 */
@RequiredArgsConstructor
public class CookieLogoutHandler implements LogoutHandler {

    private final SessionManager sessionManager;
    private final CookieAuthProperties cookieAuthProperties;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return;
        }

        Arrays.stream(cookies)
                .filter(c -> cookieAuthProperties.getCookieName().equals(c.getName()))
                .findFirst()
                .ifPresent(cookie -> {
                    // 从缓存中删除会话
                    sessionManager.removeSession(cookie.getValue());

                    // 清除客户端的 Cookie
                    Cookie clearCookie = new Cookie(cookieAuthProperties.getCookieName(), null);
                    clearCookie.setPath(cookieAuthProperties.getCookiePath());
                    clearCookie.setHttpOnly(cookieAuthProperties.getCookieHttpOnly());
                    clearCookie.setSecure(cookieAuthProperties.getCookieSecure());
                    clearCookie.setMaxAge(0);
                    if (cookieAuthProperties.getCookieDomain() != null
                            && !cookieAuthProperties.getCookieDomain().isBlank()) {
                        clearCookie.setDomain(cookieAuthProperties.getCookieDomain());
                    }
                    response.addCookie(clearCookie);
                });
    }
}
