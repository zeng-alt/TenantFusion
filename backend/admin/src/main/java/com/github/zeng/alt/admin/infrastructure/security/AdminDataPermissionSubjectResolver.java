package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import com.github.zeng.alt.security.data.DataPermissionSubject;
import com.github.zeng.alt.security.data.DataPermissionSubjectResolver;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceService;
import io.vavr.control.Either;
import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 复用可信 ABAC 主体并保留当前角色、数据库角色成员关系和逐角色权限交集。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Component
@RequiredArgsConstructor
public class AdminDataPermissionSubjectResolver implements DataPermissionSubjectResolver {
    private final AdminAbacService abac;
    private final RbacResourceService rbac;
    private final SecurityProperties properties;

    @Override
    public Either<String, DataPermissionSubject> resolve(String permission) {
        return abac.resolve(permission).map(subject -> {
            final var user = UserContextHolder.getSecurityUser();
            final var candidates = Option.of(user.getCurrentRole()).map(role -> List.of(role.getAuthority()))
                    .getOrElse(() -> user.getRoles().stream().map(GrantedAuthority::getAuthority).toList());
            final var roles = rbac.findUserRole(user.getId(), user.getTenant()).stream().filter(candidates::contains)
                    .filter(role -> rbac.findRolePermission(List.of(role), user.getId(), user.getTenant())
                            .contains(permission)).collect(Collectors.toUnmodifiableSet());
            final var admin = properties.getAdmin();
            final boolean administrator = Boolean.TRUE.equals(admin.getEnabled())
                    && (Objects.equals(admin.getId(), user.getId()) || Option.of(user.getCurrentRole())
                            .map(GrantedAuthority::getAuthority).filter(admin.getCode()::equals).isDefined());
            return new DataPermissionSubject(subject.tenant(), user.getId(),
                    Option.of((Long) subject.attributes().get("deptId")), roles, administrator);
        });
    }
}
