package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.admin.query.api.dto.AbacPermissionVO;
import com.github.zeng.alt.admin.query.api.dto.AbacPolicyCmd;
import com.github.zeng.alt.admin.query.api.dto.AbacSimulationCmd;
import com.github.zeng.alt.admin.query.api.dto.AbacSimulationVO;
import com.github.zeng.alt.security.abac.AbacFailure;
import com.github.zeng.alt.security.abac.AbacPolicy;
import com.github.zeng.alt.security.abac.client.cache.CachedAbacPolicyLoader;
import com.github.zeng.alt.security.abac.method.AbacSchemaDto;
import com.github.zeng.alt.security.abac.policy.AbacPolicyDto;
import com.github.zeng.alt.security.abac.policy.AbacPolicyQuery;
import com.github.zeng.alt.security.abac.server.AbacPolicyCreateCmd;
import com.github.zeng.alt.security.abac.server.AbacPolicyService;
import com.github.zeng.alt.security.api.AuthHelper;
import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 管理入口的身份/租户边界及策略生命周期编排。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Service
@RequiredArgsConstructor
public class AbacWorkbenchService {
    private final AbacWorkbenchRepository repository;
    private final AbacWorkbenchTemplate template;
    private final AbacPolicyService policies;
    private final CachedAbacPolicyLoader cache;
    private final AuthHelper auth;

    public List<AbacPermissionVO> listPermissions() {
        return repository.listPermissions(tenant("abac:permissions:read")).stream()
                .filter(permission -> !template.schema(permission.code()).methods().isEmpty()).toList();
    }

    public List<AbacPolicyDto> listPolicies(String permission) {
        return repository.listPolicies(tenant("abac:policies:read"), permission);
    }

    public AbacSchemaDto schema(String permission) {
        final String tenant = tenant("abac:metadata:read");
        requirePermission(tenant, permission);
        return template.schema(permission);
    }

    public AbacSimulationVO simulate(AbacSimulationCmd command) {
        final String tenant = tenant("abac:simulate");
        requirePermission(tenant, command.permission());
        return template.simulate(tenant, command);
    }

    public Either<AbacFailure, AbacPolicyDto> create(AbacPolicyCmd command) {
        final String tenant = tenant("abac:policies:create");
        return template.validate(command).flatMap(ignored -> policies.create(new AbacPolicyCreateCmd(tenant,
                command.permission(),
                command.condition(),
                command.preAuth())))
                .map(AbacPolicyDto::from);
    }

    public Either<AbacFailure, AbacPolicyDto> update(String id, AbacPolicyCmd command) {
        final String tenant = tenant("abac:policies:update");
        return template.validate(command).flatMap(ignored -> policies.update(new AbacPolicy(id,
                tenant,
                command.permission(),
                command.condition(),
                command.preAuth())))
                .map(AbacPolicyDto::from);
    }

    public Either<AbacFailure, Boolean> delete(String id) {
        return policies.delete(tenant("abac:policies:delete"), id);
    }

    public Either<AbacFailure, Boolean> refresh(String permission) {
        final String tenant = tenant("abac:policies:refresh");
        requirePermission(tenant, permission);
        return cache.refresh(List.of(new AbacPolicyQuery(tenant, permission)));
    }

    private String tenant(String permission) {
        final String tenant = TenantContextHolder.getTenantId();
        final var user = UserContextHolder.getSecurityUser();
        if (tenant == null || user == null
                || (!tenant.equals(user.getTenant()) && !tenant.equals(user.getActiveTenant()))
                || !(auth.hasAdmin() || auth.hasAuthority(permission))) {
            throw new AccessDeniedException("无权管理当前租户的 ABAC 策略");
        }
        return tenant;
    }

    private void requirePermission(String tenant, String permission) {
        if (!repository.exists(tenant, permission)) { throw new AccessDeniedException("当前租户不存在该权限"); }
    }
}
