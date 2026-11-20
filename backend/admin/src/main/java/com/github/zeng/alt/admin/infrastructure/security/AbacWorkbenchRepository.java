package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.admin.infrastructure.repository.PermissionRepository;
import com.github.zeng.alt.admin.infrastructure.repository.PolicyRuleRepository;
import com.github.zeng.alt.admin.query.api.dto.AbacPermissionVO;
import com.github.zeng.alt.security.abac.policy.AbacPolicyDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * 工作台只在显式指定的当前租户内读取权限和策略。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AbacWorkbenchRepository {
    private final PermissionRepository permissions;
    private final PolicyRuleRepository policies;
    private final AbacPolicyMapper mapper;

    public List<AbacPermissionVO> listPermissions(String tenant) {
        return permissions.findAllEnablePermissions().stream()
                .filter(permission -> tenant.equals(permission.getTenantBy()))
                .map(mapper::toPermissionVO).sorted(Comparator.comparing(AbacPermissionVO::code)).toList();
    }

    public List<AbacPolicyDto> listPolicies(String tenant, String permission) {
        return policies.listByPermission(tenant, permission).stream().map(mapper::toPolicy).map(AbacPolicyDto::from).toList();
    }

    public boolean exists(String tenant, String permission) {
        return permissions.findByTenantAndCode(tenant, permission).isDefined();
    }
}
