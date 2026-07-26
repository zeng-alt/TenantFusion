package com.github.zeng.alt.security.jwt;

import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.core.properties.LoginProperties;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtStorage jwtStorage;
    private final String headerName;
    private final RequestMatcher loginRequestMatcher;
    private final String newAccessTokenHeader;
    private final String refreshCookieName;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(
        JwtTokenProvider jwtTokenProvider,
        JwtStorage jwtStorage,
        UserDetailsService userDetailsService,
        String headerName,
        LoginProperties loginProperties,
        String newAccessTokenHeader,
        String refreshCookieName
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtStorage = jwtStorage;
        this.headerName = headerName;
        this.userDetailsService = userDetailsService;
        this.loginRequestMatcher = PathPatternRequestMatcher.withDefaults().matcher(loginProperties.getMethod(), loginProperties.getLoginPath());
        this.newAccessTokenHeader = newAccessTokenHeader;
        this.refreshCookieName = refreshCookieName;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {

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

        JwtTokenProvider.TokenParseResult parseResult =
                jwtTokenProvider.parseAccessToken(token);

        if (parseResult.result() == JwtTokenProvider.TokenValidationResult.VALID) {

            Jwt jwt = parseResult.jwt();
            String cacheKey = jwtTokenProvider.getAccessCacheKey(jwt);

            if (cacheKey == null || !jwtStorage.hasToken(cacheKey)) {
                parseResult = JwtTokenProvider.TokenParseResult.expired();
            } else {

                SecurityUser securityUser =
                        jwtTokenProvider.getUserFromClaims(jwt);
                String claimTenant = jwtTokenProvider.getClaimTenant(jwt);
                TenantContextHolder.setTenantId(claimTenant);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                securityUser,
                                null,
                                securityUser.getAuthorities()
                        );
                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
                try {
                    filterChain.doFilter(request,response);
                } finally {
                    SecurityContextHolder.clearContext();
                }
                return;
            }
        }

        if (parseResult.result() == JwtTokenProvider.TokenValidationResult.EXPIRED) {
            String refreshToken = extractRefreshTokenFromCookie(request);
            if (!StringUtils.hasText(refreshToken)) {
                filterChain.doFilter(request, response);
                return;
            }

            JwtTokenProvider.TokenParseResult refreshResult = jwtTokenProvider.parseRefreshToken(refreshToken);
            if (refreshResult.result() != JwtTokenProvider.TokenValidationResult.VALID) {
                filterChain.doFilter(request, response);
                return;
            }

            Jwt refreshJwt = refreshResult.jwt();
            String refreshCacheKey = jwtTokenProvider.getRefreshCacheKey(refreshJwt);
            if (!jwtStorage.hasToken(refreshCacheKey)) {
                filterChain.doFilter(request, response);
                return;
            }

            String subject = refreshJwt.getSubject();
            String claimTenant = jwtTokenProvider.getClaimTenant(refreshJwt);
            TenantContextHolder.setTenantId(claimTenant);
            SecurityUser securityUser = (SecurityUser) userDetailsService.loadUserByUsername(subject);

            if (securityUser == null) {
                filterChain.doFilter(request, response);
                return;
            }

            String oldCurrentRole = jwtTokenProvider.getCurrentRoleFromExpiredToken(token);
            if (oldCurrentRole != null && securityUser.getRoles() != null) {
                boolean found = securityUser.getRoles().stream()
                        .anyMatch(a -> oldCurrentRole.equalsIgnoreCase(a.getAuthority()));
                if (found) {
                    securityUser.setCurrentRole(new SimpleGrantedAuthority(oldCurrentRole));
                }
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());

            String newAccessToken = jwtTokenProvider.createToken(securityUser);

            String newAccessCacheKey = jwtTokenProvider.getAccessCacheKey(newAccessToken);
            if (StringUtils.hasText(newAccessCacheKey)) {
                jwtStorage.setAccessToken(newAccessCacheKey, securityUser.getUsername());
            }

            String oldAccessCacheKey = jwtTokenProvider.getCacheKeyFromExpiredToken(token);
            if (StringUtils.hasText(oldAccessCacheKey)) {
                jwtStorage.removeToken(oldAccessCacheKey);
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
}
