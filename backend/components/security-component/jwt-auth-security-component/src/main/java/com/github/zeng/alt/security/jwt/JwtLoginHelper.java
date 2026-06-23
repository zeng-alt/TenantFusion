package com.github.zeng.alt.security.jwt;

import com.github.zeng.alt.security.api.LoginHelper;
import com.github.zeng.alt.security.api.LoginResponse;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.storage.StorageTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Duration;

/**
 * JWT 登录认证实现.
 *
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年10月07日
 */
@RequiredArgsConstructor
public class JwtLoginHelper implements LoginHelper {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final StorageTemplate storageTemplate;
    private final long expiration;

    @Override
    public String name() {
        return "jwt";
    }

    @Override
    public LoginResponse login(String username, String password) {
        UsernamePasswordAuthenticationToken authRequest =
                UsernamePasswordAuthenticationToken.unauthenticated(username, password);
        Authentication authenticated = authenticationManager.authenticate(authRequest);
        SecurityUser user = (SecurityUser) authenticated.getPrincipal();

        String jwt = jwtTokenProvider.createToken(user);
        String cacheKey = jwtTokenProvider.getCacheKey(jwt);
        if (cacheKey != null) {
            storageTemplate.opsForString().set(cacheKey, user.getUsername(), Duration.ofSeconds(expiration));
        }

        return LoginResponse.success(user)
                .attribute("accessToken", jwt)
                .attribute("tokenType", "Bearer")
                .attribute("expiresIn", expiration);
    }

    @Override
    public void logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }
        String token = authHeader.substring(7);
        String cacheKey = jwtTokenProvider.getCacheKey(token);
        if (cacheKey != null) {
            storageTemplate.delete(cacheKey);
        }
    }

    @Override
    public SecurityUser getCurrentUser() {
        return UserContextHolder.getSecurityUser();
    }
}
