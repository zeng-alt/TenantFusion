package com.github.zeng.alt.security.jwt;

import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.core.properties.LoginProperties;
import com.github.zeng.alt.security.core.TenantSwitchTemplate;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import io.vavr.control.Option;
import io.vavr.control.Either;
import io.vavr.control.Try;
import lombok.extern.apachecommons.CommonsLog;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.core.log.LogMessage;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

@CommonsLog
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtStorage jwtStorage;
    private final String headerName;
    private final RequestMatcher loginRequestMatcher;
    private final String newAccessTokenHeader;
    private final String refreshCookieName;
    private final String refreshTokenHeader;
    private final Option<UserDetailsService> userDetailsService;
    private final Option<TenantSwitchTemplate> tenantSwitch;

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
        this.userDetailsService = Option.of(userDetailsService);
        this.tenantSwitch = Option.none();
        this.loginRequestMatcher = PathPatternRequestMatcher.withDefaults()
                .matcher(loginProperties.getMethod(), loginProperties.getLoginPath());
        this.newAccessTokenHeader = newAccessTokenHeader;
        this.refreshCookieName = refreshCookieName;
        this.refreshTokenHeader = "X-Refresh-Token";
    }

    /**
     * 签发端提供权威账户查询；只验签端依赖共享 Token 缓存，不加载本地用户或执行刷新。
     */
    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            JwtStorage jwtStorage,
            Option<UserDetailsService> userDetailsService,
            JwtProperties properties
    ) {
        this(jwtTokenProvider, jwtStorage, userDetailsService, properties, Option.none());
    }

    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            JwtStorage jwtStorage,
            Option<UserDetailsService> userDetailsService,
            JwtProperties properties,
            Option<TenantSwitchTemplate> tenantSwitch
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtStorage = jwtStorage;
        this.userDetailsService = userDetailsService;
        this.tenantSwitch = tenantSwitch;
        this.headerName = "Authorization";
        this.loginRequestMatcher = PathPatternRequestMatcher.withDefaults()
                .matcher(properties.getLogin().getMethod(), properties.getLogin().getLoginPath());
        this.newAccessTokenHeader = properties.getNewAccessTokenHeader();
        this.refreshCookieName = properties.getRefreshCookieName();
        this.refreshTokenHeader = properties.getRefreshTokenHeader();
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader(headerName);
        if (loginRequestMatcher.matches(request) || authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        final Option<SecurityUser> user = resolveUser(authHeader.substring(7), request, response);
        log.debug(LogMessage.format("JWT request: method=%s path=%s userRestored=%s",
                request.getMethod(), request.getRequestURI(), user.isDefined()));
        if (user.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        final String selectedTenant = request.getHeader("X-Active-Tenant");
        if (StringUtils.hasText(selectedTenant)) {
            final Either<String, String> selected = selectTenant(user.get(), selectedTenant.trim(), request, response);
            if (selected.isLeft()) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, selected.getLeft());
                return;
            }
            user.get().setActiveTenant(selected.get());
            setTenant(selected.get());
        }
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                user.get(), null, user.get().getAuthorities()));
        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private Option<SecurityUser> resolveUser(
            String token, HttpServletRequest request, HttpServletResponse response
    ) {
        final JwtTokenProvider.TokenParseResult parsed = jwtTokenProvider.parseAccessToken(token);
        log.debug(LogMessage.format("JWT validation: method=%s path=%s status=%s",
                request.getMethod(), request.getRequestURI(), parsed.result()));
        return switch (parsed.result()) {
            case VALID -> Option.of(jwtTokenProvider.getAccessCacheKey(parsed.jwt())).filter(jwtStorage::hasToken)
                    .peek(key -> log.debug("JWT access token cache hit"))
                    .onEmpty(() -> log.debug("JWT access token cache miss"))
                    .isDefined() ? accessUser(parsed.jwt()) : refreshUser(token, request, response);
            case EXPIRED -> refreshUser(token, request, response);
            case INVALID -> Option.none();
        };
    }

    private Option<SecurityUser> accessUser(Jwt jwt) {
        setTenant(jwtTokenProvider.getClaimTenant(jwt));
        if (userDetailsService.isEmpty()) {
            return Option.of(jwtTokenProvider.getUserFromClaims(jwt));
        }
        return loadUser(jwt).peek(user -> restoreCurrentRole(user, jwt.getClaimAsString("currentRole")));
    }

    private Option<SecurityUser> loadUser(Jwt jwt) {
        return userDetailsService.flatMap(service -> Try.of(
                        () -> Option.of(service.loadUserByUsername(jwt.getSubject())))
                        .recover(UsernameNotFoundException.class, ignored -> Option.none()).get())
                .filter(SecurityUser.class::isInstance).map(SecurityUser.class::cast)
                .peek(user -> log.debug(LogMessage.format(
                        "JWT user verification: active=%s idMatches=%s tenantMatches=%s",
                        user.isEnabled() && user.isAccountNonLocked()
                                && user.isAccountNonExpired() && user.isCredentialsNonExpired(),
                        Objects.equals(jwt.getClaimAsString("id"), user.getId()),
                        Objects.equals(jwtTokenProvider.getClaimTenant(jwt), user.getTenant())
                )))
                .filter(user -> user.isEnabled() && user.isAccountNonLocked()
                        && user.isAccountNonExpired() && user.isCredentialsNonExpired())
                .filter(user -> Objects.equals(jwt.getClaimAsString("id"), user.getId())
                        && Objects.equals(jwtTokenProvider.getClaimTenant(jwt), user.getTenant()))
                .peek(SecurityUser::eraseCredentials);
    }

    private Option<SecurityUser> refreshUser(
            String oldToken, HttpServletRequest request, HttpServletResponse response
    ) {
        if (userDetailsService.isEmpty()) {
            return Option.none();
        }
        return refreshToken(request).map(jwtTokenProvider::parseRefreshToken)
                .filter(parsed -> parsed.result() == JwtTokenProvider.TokenValidationResult.VALID)
                .map(JwtTokenProvider.TokenParseResult::jwt)
                .filter(jwt -> jwtStorage.hasToken(jwtTokenProvider.getRefreshCacheKey(jwt)))
                .peek(jwt -> setTenant(jwtTokenProvider.getClaimTenant(jwt)))
                .flatMap(this::loadUser)
                .peek(user -> restoreCurrentRole(user, jwtTokenProvider.getCurrentRoleFromExpiredToken(oldToken)))
                .peek(user -> refreshAccessToken(user, oldToken, response));
    }

    private void restoreCurrentRole(SecurityUser user, String currentRole) {
        Option.of(currentRole).filter(role -> user.getRoles().stream()
                        .anyMatch(authority -> role.equalsIgnoreCase(authority.getAuthority())))
                .peek(role -> user.setCurrentRole(new SimpleGrantedAuthority(role)));
    }

    private void refreshAccessToken(SecurityUser user, String oldToken, HttpServletResponse response) {
        final String replacement = jwtTokenProvider.createToken(user);
        Option.of(jwtTokenProvider.getAccessCacheKey(replacement))
                .peek(key -> jwtStorage.setAccessToken(key, user.getUsername()));
        Option.of(jwtTokenProvider.getCacheKeyFromExpiredToken(oldToken)).peek(jwtStorage::removeToken);
        response.setHeader(newAccessTokenHeader, replacement);
    }

    private Option<String> refreshToken(HttpServletRequest request) {
        return Option.of(request.getHeader(refreshTokenHeader)).filter(StringUtils::hasText)
                .orElse(() -> Option.of(request.getCookies()).map(Arrays::stream)
                        .flatMap(cookies -> Option.ofOptional(cookies
                                .filter(cookie -> refreshCookieName.equals(cookie.getName()))
                                .map(Cookie::getValue).findFirst())));
    }

    private void setTenant(String tenant) {
        TenantContextHolder.setTenantId(tenant);
        TenantContextHolder.setRouting(null);
        TenantContextHolder.setDatabase(null);
        TenantContextHolder.setSchema(null);
    }

    private Either<String, String> selectTenant(
            SecurityUser user, String tenant, HttpServletRequest request, HttpServletResponse response
    ) {
        if (RequestContextHolder.getRequestAttributes() != null) {
            return tenantSwitch.toEither("无权切换租户").flatMap(template -> template.select(user, tenant));
        }
        final var attributes = new ServletRequestAttributes(request, response);
        RequestContextHolder.setRequestAttributes(attributes);
        try {
            return tenantSwitch.toEither("无权切换租户").flatMap(template -> template.select(user, tenant));
        } finally {
            RequestContextHolder.resetRequestAttributes();
            attributes.requestCompleted();
        }
    }
}
