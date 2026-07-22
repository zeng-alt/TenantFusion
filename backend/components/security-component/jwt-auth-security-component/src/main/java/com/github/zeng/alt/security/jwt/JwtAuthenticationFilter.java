package com.github.zeng.alt.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.core.properties.LoginProperties;
import com.github.zeng.alt.storage.StorageTemplate;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final StorageTemplate storageTemplate;
    private final ObjectMapper objectMapper;
    private final String headerName;
    private final RequestMatcher loginRequestMatcher;
    private final String newAccessTokenHeader;
    private final String refreshCookieName;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   StorageTemplate storageTemplate,
                                   ObjectMapper objectMapper,
                                   String headerName,
                                   LoginProperties loginProperties,
                                   String newAccessTokenHeader,
                                   String refreshCookieName) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.storageTemplate = storageTemplate;
        this.objectMapper = objectMapper;
        this.headerName = headerName;
        this.loginRequestMatcher = PathPatternRequestMatcher.withDefaults().matcher(loginProperties.getMethod(), loginProperties.getLoginPath());
        this.newAccessTokenHeader = newAccessTokenHeader;
        this.refreshCookieName = refreshCookieName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (loginRequestMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(headerName);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        JwtTokenProvider.TokenValidationResult result = jwtTokenProvider.validateTokenWithResult(token);

        if (result == JwtTokenProvider.TokenValidationResult.VALID) {
            String cacheKey = jwtTokenProvider.getCacheKey(token);
            if (cacheKey == null || Boolean.FALSE.equals(storageTemplate.hasKey(cacheKey))) {
                result = JwtTokenProvider.TokenValidationResult.EXPIRED;
            } else {
                Jwt jwt = jwtTokenProvider.getJwt(token);
                if (jwt == null) {
                    filterChain.doFilter(request, response);
                    return;
                }

                SecurityUser securityUser = jwtTokenProvider.getUserFromClaims(jwt);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);

                try {
                    filterChain.doFilter(request, response);
                } finally {
                    SecurityContextHolder.clearContext();
                }
                return;
            }
        }

        if (result == JwtTokenProvider.TokenValidationResult.EXPIRED) {
            String refreshToken = extractRefreshTokenFromCookie(request);
            if (StringUtils.hasText(refreshToken) && jwtTokenProvider.validateRefreshToken(refreshToken)) {

                Jwt refreshJwt = jwtTokenProvider.getRefreshJwt(refreshToken);
                String userId = refreshJwt.getSubject();

                String userCacheKey = jwtTokenProvider.getUserCacheKey(userId);
                String userInfoJson = storageTemplate.opsForString().get(userCacheKey, String.class);

                if (userInfoJson == null) {
                    filterChain.doFilter(request, response);
                    return;
                }

                SecurityUser securityUser = buildUserFromCache(userInfoJson);
                if (securityUser == null) {
                    filterChain.doFilter(request, response);
                    return;
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());

                String newAccessToken = jwtTokenProvider.createToken(securityUser);

                String newAccessCacheKey = jwtTokenProvider.getCacheKey(newAccessToken);
                if (newAccessCacheKey != null) {
                    storageTemplate.opsForString().set(
                            newAccessCacheKey,
                            securityUser.getUsername(),
                            Duration.ofSeconds(jwtTokenProvider.getExpirationSeconds())
                    );
                }

                String oldAccessCacheKey = jwtTokenProvider.getCacheKeyFromExpiredToken(token);
                if (oldAccessCacheKey != null) {
                    storageTemplate.delete(oldAccessCacheKey);
                }

                response.setHeader(newAccessTokenHeader, newAccessToken);

                SecurityContextHolder.getContext().setAuthentication(authentication);

                try {
                    filterChain.doFilter(request, response);
                } finally {
                    SecurityContextHolder.clearContext();
                }
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (refreshCookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private SecurityUser buildUserFromCache(String json) {
        try {
            Map<String, Object> userInfo = objectMapper.readValue(json, Map.class);
            String id = (String) userInfo.get("id");
            String username = (String) userInfo.get("username");
            String tenant = (String) userInfo.get("tenant");
            String currentRoleStr = (String) userInfo.get("currentRole");
            List<String> roles = (List<String>) userInfo.get("roles");

            Set<GrantedAuthority> authorities = roles == null
                    ? Set.of()
                    : roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet());

            return new SecurityUser(
                    id,
                    username,
                    "",
                    tenant != null && !tenant.isEmpty() ? tenant : null,
                    null,
                    null,
                    true,
                    true,
                    true,
                    true,
                    authorities,
                    currentRoleStr != null && !currentRoleStr.isEmpty()
                            ? new SimpleGrantedAuthority(currentRoleStr)
                            : null,
                    null
            );
        } catch (Exception e) {
            logger.warn("Failed to build user from cache", e);
            return null;
        }
    }
}
