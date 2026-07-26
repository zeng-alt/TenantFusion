package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.admin.infrastructure.entity.*;
import com.github.zeng.alt.security.api.HttpResource;
import com.github.zeng.alt.security.api.Resource;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceLoader;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class RbacResourceLoaderImpl implements RbacResourceLoader {

    private final EntityManager entityManager;

    @Override
    public String loadPermissionByResource(String tenantName, Resource resource) {
        if (resource instanceof HttpResource httpResource) {
            JPAQueryFactory qf = new JPAQueryFactory(entityManager);
            QHttpResource http = QHttpResource.httpResource;

            String code = qf
                    .select(http.code)
                    .from(http)
                    .where(http.path.eq(httpResource.getUri()))
                    .where(http.method.eq(httpResource.getMethod()))
                    .where(http.enabled.isTrue())
                    .fetchOne();

            log.debug("Permission for {}:{} -> {}", httpResource.getMethod(), httpResource.getUri(), code);
            return code;
        }

        return null;

    }

    @Override
    public Map<String, Set<String>> loadPermissions(List<String> authorities, String tenantName) {
        if (CollectionUtils.isEmpty(authorities)) {
            return Collections.emptyMap();
        }

        JPAQueryFactory qf = new JPAQueryFactory(entityManager);
        QRole role = QRole.role;
        QRolePermission rp = QRolePermission.rolePermission;
        QPermission perm = QPermission.permission;

        List<Tuple> rows = qf
                .select(role.code, perm.code)
                .from(perm)
                .join(perm.rolePermissions, rp)
                .join(rp.role, role)
                .where(role.code.in(authorities))
                .where(perm.enabled.isTrue())
                .fetch();

        Map<String, Set<String>> result = new HashMap<>();
        for (String a : authorities) {
            result.put(a, new HashSet<>());
        }
        for (Tuple row : rows) {
            String roleCode = row.get(role.code);
            String permCode = row.get(perm.code);
            if (roleCode != null && permCode != null) {
                result.computeIfAbsent(roleCode, k -> new HashSet<>()).add(permCode);
            }
        }

        long total = result.values().stream().mapToInt(Set::size).sum();
        log.debug("Loaded {} permissions across {} authorities", total, authorities.size());
        return result;
    }

    @Override
    public Set<String> loadUserRole(String userId, String tenantName) {
        JPAQueryFactory qf = new JPAQueryFactory(entityManager);

        QUser user = QUser.user;
        QUserRole userRole = QUserRole.userRole;
        QRole role = QRole.role;

        List<String> rows = qf
                .select(role.code)
                .from(user)
                .join(user.userRoles, userRole)
                .join(userRole.role, role)
                .where(
                        user.userId.eq(Long.parseLong(userId)),
                        user.enabled.isTrue(),
                        role.enabled.isTrue()
                )
                .fetch();

        return new HashSet<>(rows);
    }
}
