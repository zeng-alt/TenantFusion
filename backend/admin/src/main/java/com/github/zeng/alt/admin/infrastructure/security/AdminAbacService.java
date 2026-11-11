package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.admin.infrastructure.repository.UserRepository;
import com.github.zeng.alt.security.abac.AbacContext;
import com.github.zeng.alt.security.abac.AbacFailure;
import com.github.zeng.alt.security.abac.AbacMethodContext;
import com.github.zeng.alt.security.abac.AbacTemplate;
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
 * 所有资源属性由业务层提供，不从请求体或请求头复制授权属性。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Service
@RequiredArgsConstructor
public class AdminAbacService implements AbacSubjectProvider, AbacEnvironmentProvider {
    private static final String USER_UPDATE_PATH = "/v1/user/{id}";
    private final AbacTemplate template;
    private final RbacResourceService rbac;
    private final UserRepository users;
    private final SecurityProperties properties;
    private final AbacUserMapper mapper;

    public Either<String, Boolean> authorize(
            Authentication authentication,
            String permission,
            Map<String, Object> resource,
            Map<String, Object> environment
    ) {
        return principal(authentication).flatMap(user -> {
            if (resource.containsKey("tenant") && !user.getTenant().equals(resource.get("tenant"))) {
                return Either.left("资源不属于当前租户");
            }
            return Try.of(() -> hasPermission(user, permission)).toEither()
                    .mapLeft(ignored -> "基础权限加载失败")
                    .flatMap(granted -> granted ? subject(user) : Either.left("没有访问权限"))
                    .flatMap(subject -> Try.of(() -> new AbacContext(
                            user.getTenant(), permission, subject.attributes(), resource, environment
                    )).toEither().mapLeft(ignored -> "授权属性无效"))
                    .flatMap(context -> template.authorize(context).get().mapLeft(AbacFailure::message));
        });
    }

    /** 方法级主体解析：认证 + 租户一致 + RBAC 预检 + 可信主体属性，供 @AbacAuthorize 拦截器调用。 */
    @Override
    public Either<String, AbacSubject> resolve(String permission) {
        return principal(UserContextHolder.getAuthentication())
                .flatMap(user -> Try.of(() -> hasPermission(user, permission)).toEither()
                        .mapLeft(ignored -> "基础权限加载失败")
                        .flatMap(granted -> granted ? Either.right(user) : Either.left("没有访问权限")))
                .flatMap(user -> subject(user).map(dto -> new AbacSubject(user.getTenant(), dto.attributes())));
    }

    /** admin 当前策略不引用环境属性，贡献空环境；后续可在此补充全局环境快照。 */
    @Override
    public Map<String, Object> environment(AbacMethodContext context) {
        return Map.of();
    }

    /** 用户更新路由读取数据库资源；其他路由仅开放方法和路径，不猜测业务 ID。 */
    public Either<String, Boolean> authorizeHttp(Authentication authentication, String method, String path, String route) {
        return principal(authentication).flatMap(user -> permission(user, method, route))
                .flatMap(code -> resource(method, path, route)
                        .flatMap(attributes -> authorize(authentication, code, attributes,
                                Map.of("action", method, "path", route))));
    }

    private Either<String, SecurityUser> principal(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof SecurityUser user) || !user.isEnabled()) {
            return Either.left("当前用户未登录或已禁用");
        }
        if (!StringUtils.hasText(user.getTenant()) || !user.getTenant().equals(TenantContextHolder.getTenantId())) {
            return Either.left("认证租户与当前数据租户不一致");
        }
        return Either.right(user);
    }

    private boolean hasPermission(SecurityUser user, String permission) {
        final var admin = properties.getAdmin();
        if (Boolean.TRUE.equals(admin.getEnabled()) && (Objects.equals(admin.getId(), user.getId())
                || Option.of(user.getCurrentRole()).map(GrantedAuthority::getAuthority)
                        .filter(admin.getCode()::equals).isDefined())) {
            return true;
        }
        final List<String> roles = Option.of(user.getCurrentRole())
                .map(role -> List.of(role.getAuthority()))
                .getOrElse(() -> user.getRoles().stream().map(GrantedAuthority::getAuthority).toList());
        return rbac.findRolePermission(roles, user.getId(), user.getTenant()).contains(permission);
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

    private Either<String, Map<String, Object>> resource(String method, String path, String route) {
        if (!"PATCH".equals(method) || !USER_UPDATE_PATH.equals(route)) {
            return Either.right(Map.of("path", route));
        }
        return Try.of(() -> Long.parseLong(path.substring(path.lastIndexOf('/') + 1)))
                .toEither().mapLeft(ignored -> "资源标识无效")
                .flatMap(id -> Try.of(() -> users.findById(id)).toEither().mapLeft(ignored -> "资源属性加载失败"))
                .flatMap(found -> found.toEither("目标用户不存在"))
                .map(user -> mapper.toDto(user).attributes());
    }
}
