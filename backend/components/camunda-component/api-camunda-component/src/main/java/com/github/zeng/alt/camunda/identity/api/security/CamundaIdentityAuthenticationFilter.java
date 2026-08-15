package com.github.zeng.alt.camunda.identity.api.security;

import com.github.zeng.alt.security.api.SecurityUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.camunda.bpm.engine.IdentityService;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 将当前 Spring Security 认证用户同步到 Camunda 引擎的 {@link IdentityService}。
 * <p>
 * 与 {@code JwtAuthenticationFilter} 的职责类似：每次请求从 {@link SecurityContextHolder}
 * 中取出已认证的 {@link SecurityUser}，通过 {@link IdentityService#setAuthentication}
 * 设置到 Camunda 引擎的线程上下文，使流程引擎的授权/历史操作能感知当前操作人，
 * 并在请求结束后清理。用户、角色（组）、租户均直接取自 {@link SecurityUser}，
 * 无需再向 admin 服务发起额外查询。
 */
public class CamundaIdentityAuthenticationFilter extends OncePerRequestFilter {

    private final IdentityService identityService;

    public CamundaIdentityAuthenticationFilter(IdentityService identityService) {
        this.identityService = identityService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        SecurityUser securityUser = resolveSecurityUser();
        if (securityUser == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String userId = securityUser.getUsername();
        List<String> groupIds = resolveGroupIds(securityUser);
        List<String> tenantIds = StringUtils.hasText(securityUser.getTenant())
                ? List.of(securityUser.getTenant())
                : List.of();


        identityService.setAuthentication(userId, groupIds, tenantIds);
        identityService.setAuthenticatedUserId(userId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            identityService.clearAuthentication();
        }
    }

    private SecurityUser resolveSecurityUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof SecurityUser securityUser) {
            return securityUser;
        }
        return null;
    }

    private List<String> resolveGroupIds(SecurityUser securityUser) {
        if (securityUser.getRoles() == null) {
            return List.of();
        }
        return securityUser.getRoles().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(StringUtils::hasText)
                .toList();
    }
}