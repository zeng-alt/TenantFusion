package com.github.zeng.alt.security.cookie;

import com.github.zeng.alt.security.api.LoginHelper;
import com.github.zeng.alt.security.api.LoginResponse;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.security.core.web.support.LoginLogHandler;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;

import java.util.Arrays;

/**
 * Cookie 登录认证实现.
 *
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年10月07日
 */
@RequiredArgsConstructor
public class CookieLoginHelper implements LoginHelper {

    private final AuthenticationManager authenticationManager;
    private final SessionManager sessionManager;
    private final CookieAuthProperties cookieAuthProperties;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 保留手动构造的兼容入口；需要登录审计时请传入 ApplicationEventPublisher。
     */
    public CookieLoginHelper(
            AuthenticationManager authenticationManager,
            SessionManager sessionManager,
            CookieAuthProperties cookieAuthProperties
    ) {
        this(authenticationManager, sessionManager, cookieAuthProperties, event -> { });
    }

    @Override
    public String name() {
        return "cookie";
    }

    @Override
    public LoginResponse login(String username, String password) {
        final SecurityUser user = new LoginLogHandler(eventPublisher)
                .authenticate(authenticationManager, username, password);

        String sessionId = sessionManager.createSession(user);

        return LoginResponse.ofUser(user)
                .attribute("sessionId", sessionId)
                .attribute("cookieName", cookieAuthProperties.getCookieName())
                .attribute("expiresIn", cookieAuthProperties.getExpiration());
    }

    @Override
    public void logout(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return;
        }
        Arrays.stream(cookies)
                .filter(c -> cookieAuthProperties.getCookieName().equals(c.getName()))
                .findFirst()
                .ifPresent(c -> sessionManager.removeSession(c.getValue()));
    }

    @Override
    public void logout(String id) {
        sessionManager.removeUserSessions(id, UserContextHolder.getTenant());
    }

    @Override
    public SecurityUser getCurrentUser() {
        return UserContextHolder.getSecurityUser();
    }
}
