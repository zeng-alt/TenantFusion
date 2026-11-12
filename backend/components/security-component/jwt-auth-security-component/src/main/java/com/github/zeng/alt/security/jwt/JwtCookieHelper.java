package com.github.zeng.alt.security.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 统一 Refresh Cookie 的签发属性。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
final class JwtCookieHelper {
    private JwtCookieHelper() {
    }

    static Cookie create(JwtProperties properties, HttpServletRequest request, String value, int maxAge) {
        final var cookie = new Cookie(properties.getRefreshCookieName(), value);
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        cookie.setPath(properties.getRefreshCookiePath());
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", properties.getRefreshCookieSameSite());
        return cookie;
    }
}
