package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.admin.infrastructure.entity.*;
import com.github.zeng.alt.security.api.Resource;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceLoader;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class RbacResourceLoaderImpl implements RbacResourceLoader {

    private final EntityManager entityManager;

    @Override
    public List<Resource> loadHttpResources(String username, String tenantName, List<String> authorities) {
        if (!StringUtils.hasText(username)) {
            log.warn("loadHttpResources called with empty username");
            return Collections.emptyList();
        }
        JPAQueryFactory qf = new JPAQueryFactory(entityManager);

        QHttpResource http = QHttpResource.httpResource;
        QRolePermission rp = QRolePermission.rolePermission;
        QRole role = QRole.role;
        QUserRole ur = QUserRole.userRole;
        QUser user = QUser.user;
        QUserResource uRes = QUserResource.userResource;

        Set<Long> ids = new HashSet<>();

        ids.addAll(qf
                .select(http.permissionId)
                .from(http)
                .join(http.rolePermissions, rp)
                .join(rp.role, role)
                .join(role.userRoles, ur)
                .join(ur.user, user)
                .where(user.username.eq(username))
                .where(http.enabled.isTrue())
                .fetch());

        List<Long> directPermIds = qf
                .select(uRes.resource.permissionId)
                .from(uRes)
                .join(uRes.user, user)
                .where(user.username.eq(username))
                .fetch();
        if (!CollectionUtils.isEmpty(directPermIds)) {
            ids.addAll(qf
                    .select(http.permissionId)
                    .from(http)
                    .where(http.permissionId.in(directPermIds))
                    .where(http.enabled.isTrue())
                    .fetch());
        }

        if (ids.isEmpty()) {
            log.debug("No HTTP resources found for user '{}'", username);
            return Collections.emptyList();
        }

        List<com.github.zeng.alt.admin.infrastructure.entity.HttpResource> entities = qf
                .selectFrom(http)
                .where(http.permissionId.in(ids))
                .fetch();

        log.debug("Loaded {} HTTP resources for user '{}'", entities.size(), username);
        return entities.stream()
                .map(e -> {
                    com.github.zeng.alt.security.api.HttpResource r =
                            new com.github.zeng.alt.security.api.HttpResource();
                    r.setUri(e.getPath());
                    r.setMethod(e.getMethod());
                    return (Resource) r;
                })
                .toList();
    }

    @Override
    public String loadPermissionByResource(String tenantName, String resourceKey) {
        if (!StringUtils.hasText(resourceKey)) {
            return null;
        }
        int idx = resourceKey.lastIndexOf(':');
        if (idx <= 0 || idx == resourceKey.length() - 1) {
            log.warn("Invalid resource key format: {}", resourceKey);
            return null;
        }
        String uri = resourceKey.substring(0, idx);
        String method = resourceKey.substring(idx + 1);
        return loadPermissionByMethodAndPath(tenantName, method, uri);
    }

    @Override
    public String loadPermissionByMethodAndPath(String tenantName, String method, String path) {
        JPAQueryFactory qf = new JPAQueryFactory(entityManager);
        QHttpResource http = QHttpResource.httpResource;

        String code = qf
                .select(http.code)
                .from(http)
                .where(http.path.eq(path))
                .where(http.method.eq(method))
                .where(http.enabled.isTrue())
                .fetchOne();

        log.debug("Permission for {}:{} -> {}", method, path, code);
        return code;
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
}
