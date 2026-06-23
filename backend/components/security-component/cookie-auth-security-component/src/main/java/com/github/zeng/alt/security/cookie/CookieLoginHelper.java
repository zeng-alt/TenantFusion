package com.github.zeng.alt.security.cookie;

import com.github.zeng.alt.security.api.LoginHelper;
import com.github.zeng.alt.security.api.LoginResponse;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.api.UserContextHolder;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

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

    @Override
    public String name() {
        return "cookie";
    }

    @Override
    public LoginResponse login(String username, String password) {
        UsernamePasswordAuthenticationToken authRequest =
                UsernamePasswordAuthenticationToken.unauthenticated(username, password);
        Authentication authenticated = authenticationManager.authenticate(authRequest);
        SecurityUser user = (SecurityUser) authenticated.getPrincipal();

        String sessionId = sessionManager.createSession(user);

        return LoginResponse.success(user)
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
    public SecurityUser getCurrentUser() {
        return UserContextHolder.getSecurityUser();
    }
}
