package com.github.zeng.alt.security.cookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * 统一会话 Cookie 的签发和清除属性。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
final class CookieHelper {
    private CookieHelper() {
    }

    static Cookie create(CookieAuthProperties properties, HttpServletRequest request, String value, int maxAge) {
        final var cookie = new Cookie(properties.getCookieName(), value);
        cookie.setPath(properties.getCookiePath());
        cookie.setHttpOnly(properties.getCookieHttpOnly());
        cookie.setSecure(Boolean.TRUE.equals(properties.getCookieSecure()) || request.isSecure());
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", properties.getCookieSameSite());
        if (StringUtils.hasText(properties.getCookieDomain())) {
            cookie.setDomain(properties.getCookieDomain());
        }
        return cookie;
    }
}
