package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.admin.infrastructure.entity.DataPermissionEntity;
import com.github.zeng.alt.admin.infrastructure.repository.PolicyRuleRepository;
import com.github.zeng.alt.security.abac.core.AbacProperties;
import com.github.zeng.alt.security.data.DataPermissionPolicyLoader;
import com.github.zeng.alt.security.data.DataPermissionRule;
import com.github.zeng.alt.security.data.DataPermissionSubject;
import io.vavr.control.Either;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 按当前租户、查询权限和授权角色加载规则；未实现的资源 ABAC 查询约束明确拒绝。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Component
@RequiredArgsConstructor
public class JpaDataPermissionPolicyLoader implements DataPermissionPolicyLoader {
    private final EntityManager entityManager;
    private final PolicyRuleRepository abacPolicies;
    private final AbacProperties abacProperties;

    @Override
    public Either<String, List<DataPermissionRule>> list(
            String resource,
            String permission,
            DataPermissionSubject subject
    ) {
        if (!abacPolicies.listByPermission(subject.tenant(), permission).isEmpty()
                || abacProperties.getRequiredPermissions().contains(permission)) {
            return Either.left("此查询的 ABAC 约束尚不支持下推，已拒绝查询");
        }
        if (subject.roles().isEmpty()) {
            return Either.right(List.of());
        }
        return Either.right(entityManager.createQuery("""
                select distinct rule from DataPermissionEntity rule left join fetch rule.departmentIds
                where rule.tenantBy = :tenant and rule.resource = :resource and rule.permission = :permission
                  and rule.roleCode in :roles and rule.enabled = true
                """, DataPermissionEntity.class)
                .setParameter("tenant", subject.tenant()).setParameter("resource", resource)
                .setParameter("permission", permission).setParameter("roles", subject.roles())
                .getResultList().stream().map(rule -> new DataPermissionRule(rule.getScope(), rule.getDepartmentIds()))
                .toList());
    }
}
