package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.admin.infrastructure.repository.UserRepository;
import com.github.zeng.alt.security.abac.AbacMethodContext;
import com.github.zeng.alt.security.abac.method.AbacEnvironmentProvider;
import com.github.zeng.alt.security.abac.method.AbacSubject;
import com.github.zeng.alt.security.abac.method.AbacSubjectProvider;
import com.github.zeng.alt.security.api.HttpResource;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceService;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 宿主授权入口：认证/租户校验 → RBAC → 可信主体属性 → ABAC。
 * HTTP 管理员通过基础校验后豁免权限检查；方法级授权继续执行属性策略。
 * 所有资源属性由业务层提供，不从请求体或请求头复制授权属性。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Service
@RequiredArgsConstructor
public class AdminAbacService implements AbacSubjectProvider, AbacEnvironmentProvider {
    private final RbacResourceService rbac;
    private final UserRepository users;
    private final SecurityProperties properties;
    private final AbacUserMapper mapper;

    /** 方法级主体解析：认证 + 租户一致 + RBAC 预检 + 可信主体属性，供 @AbacAuthorize 拦截器调用。 */
    @Override
    public Either<String, AbacSubject> resolve(String permission) {
        return getAuthenticatedUser(UserContextHolder.getAuthentication())
                .flatMap(user -> Try.of(() -> hasPermission(user, permission)).toEither()
                        .mapLeft(ignored -> "基础权限加载失败")
                        .flatMap(granted -> granted ? Either.right(user) : Either.left("没有访问权限")))
                .flatMap(user -> subject(user).map(dto -> new AbacSubject(user.getTenant(), principal(user, dto), authentication(user))));
    }

    /** admin 当前策略不引用环境属性，贡献空环境；后续可在此补充全局环境快照。 */
    @Override
    public Map<String, Object> environment(AbacMethodContext context) {
        final var attributes = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (attributes instanceof org.springframework.web.context.request.ServletRequestAttributes servlet) {
            return Map.of("method", servlet.getRequest().getMethod(), "path", servlet.getRequest().getRequestURI(),
                    "hour", java.time.LocalDateTime.now().getHour());
        }
        return Map.of("method", "INTERNAL", "path", context.method().getName(),
                "hour", java.time.LocalDateTime.now().getHour());
    }

    /** HTTP 只校验账户、租户与 RBAC；属性策略统一由方法代理执行。 */
    public Either<String, Boolean> authorizeHttp(Authentication authentication, String method, String route) {
        return getAuthenticatedUser(authentication).flatMap(user -> {
            if (hasAdmin(user)) {
                return Either.right(true);
            }
            return permission(user, method, route).flatMap(code -> Try.of(() -> hasPermission(user, code))
                    .toEither().mapLeft(ignored -> "基础权限加载失败")
                    .flatMap(granted -> granted ? Either.right(true) : Either.left("没有访问权限")));
        });
    }

    /** 校验登录用户与当前租户，不加载 RBAC 权限或执行 ABAC 策略。 */
    public Either<String, SecurityUser> getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof SecurityUser user) || !user.isEnabled()) {
            return Either.left("当前用户未登录或已禁用");
        }
        if (!StringUtils.hasText(user.getTenant()) || !user.getTenant().equals(TenantContextHolder.getTenantId())) {
            return Either.left("认证租户与当前数据租户不一致");
        }
        return Either.right(user);
    }

    private Map<String, Object> principal(SecurityUser user, AbacUserDto dto) {
        final Map<String, Object> attributes = new java.util.LinkedHashMap<>(dto.attributes());
        attributes.put("username", user.getUsername());
        return attributes;
    }

    private Map<String, Object> authentication(SecurityUser user) {
        final List<String> roles = Option.of(user.getCurrentRole()).map(role -> List.of(role.getAuthority()))
                .getOrElse(() -> user.getRoles().stream().map(GrantedAuthority::getAuthority).toList());
        final java.util.Set<String> permissions = hasAdmin(user) || roles.isEmpty() ? java.util.Set.of()
                : rbac.findRolePermission(roles, user.getId(), user.getTenant());
        final List<String> authorities = java.util.stream.Stream.concat(roles.stream(), permissions.stream()).distinct().toList();
        return Map.of("name", user.getUsername(), "authenticated", true, "roles", roles, "authorities", authorities);
    }

    private boolean hasPermission(SecurityUser user, String permission) {
        if (hasAdmin(user)) {
            return true;
        }
        final List<String> roles = Option.of(user.getCurrentRole())
                .map(role -> List.of(role.getAuthority()))
                .getOrElse(() -> user.getRoles().stream().map(GrantedAuthority::getAuthority).toList());
        return rbac.findRolePermission(roles, user.getId(), user.getTenant()).contains(permission);
    }

    private boolean hasAdmin(SecurityUser user) {
        final SecurityProperties.AdminRole admin = properties.getAdmin();
        return Boolean.TRUE.equals(admin.getEnabled()) && (Objects.equals(admin.getId(), user.getId())
                || Option.of(user.getCurrentRole()).map(GrantedAuthority::getAuthority)
                        .filter(admin.getCode()::equalsIgnoreCase).isDefined());
    }

    private Either<String, AbacUserDto> subject(SecurityUser user) {
        return Try.of(() -> Long.parseLong(user.getId())).toEither().mapLeft(ignored -> "用户标识无效")
                .flatMap(id -> Try.of(() -> users.findById(id)).toEither().mapLeft(ignored -> "用户属性加载失败"))
                .flatMap(found -> found.toEither("当前用户不存在"))
                .map(mapper::toDto)
                .filterOrElse(snapshot -> snapshot.enabled() && user.getTenant().equals(snapshot.tenant()),
                        ignored -> "当前用户不可用或租户不匹配");
    }

    private Either<String, String> permission(SecurityUser user, String method, String route) {
        if (!StringUtils.hasText(route)) {
            return Either.left("未找到请求路由");
        }
        return Try.of(() -> rbac.findPermission(user.getTenant(), HttpResource.of(route, method)))
                .toEither().mapLeft(ignored -> "请求权限加载失败")
                .flatMap(code -> Option.of(code).filter(StringUtils::hasText).toEither("未配置请求权限"));
    }

}
