package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.admin.infrastructure.entity.Permission;
import com.github.zeng.alt.admin.infrastructure.entity.PolicyRule;
import com.github.zeng.alt.admin.infrastructure.repository.PermissionRepository;
import com.github.zeng.alt.admin.infrastructure.repository.PolicyRuleRepository;
import com.github.zeng.alt.security.abac.AbacFailure;
import com.github.zeng.alt.security.abac.AbacPolicy;
import com.github.zeng.alt.security.abac.server.AbacPolicyCreateCmd;
import com.github.zeng.alt.security.abac.server.spi.AbacPolicyStore;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 策略写入契约的 JPA 实现，复用宿主既有 main_policy_rule 表，不新增表映射。
 * 创建时显式落租户列（与 Hibernate {@code @TenantId} 插入回填一致），保证返回快照携带租户；
 * 删除走实体软删除。权限停用期间写入的策略不参与授权（读取侧过滤），启用后生效。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Component
@RequiredArgsConstructor
public class JpaAbacPolicyStore implements AbacPolicyStore {

    private final PolicyRuleRepository repository;
    private final PermissionRepository permissions;
    private final AbacPolicyMapper mapper;

    @Override
    public Option<AbacPolicy> findById(String tenant, String id) {
        if (!tenant.equals(TenantContextHolder.getTenantId())) {
            return Option.none();
        }
        return findRule(id).map(mapper::toPolicy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Either<AbacFailure, AbacPolicy> create(AbacPolicyCreateCmd cmd) {
        if (!cmd.tenant().equals(TenantContextHolder.getTenantId())) {
            return Either.left(new AbacFailure("TENANT_MISMATCH", "策略租户与当前租户不一致"));
        }
        return findPermission(cmd.tenant(), cmd.permission())
                .map(permission -> {
                    final PolicyRule rule = mapper.toEntity(cmd);
                    rule.setTenantBy(cmd.tenant());
                    rule.setPermission(permission);
                    return mapper.toPolicy(repository.save(rule));
                });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Either<AbacFailure, AbacPolicy> update(AbacPolicy policy) {
        if (!policy.tenant().equals(TenantContextHolder.getTenantId())) {
            return Either.left(new AbacFailure("TENANT_MISMATCH", "策略租户与当前租户不一致"));
        }
        return findRule(policy.id())
                .toEither(() -> new AbacFailure("POLICY_NOT_FOUND", "策略不存在"))
                .flatMap(rule -> findPermission(policy.tenant(), policy.permission())
                        .map(permission -> {
                            mapper.merge(policy, rule);
                            rule.setPermission(permission);
                            return mapper.toPolicy(repository.save(rule));
                        }));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Either<AbacFailure, Boolean> delete(String tenant, String id) {
        if (!tenant.equals(TenantContextHolder.getTenantId())) {
            return Either.left(new AbacFailure("TENANT_MISMATCH", "策略租户与当前租户不一致"));
        }
        return findRule(id)
                .toEither(() -> new AbacFailure("POLICY_NOT_FOUND", "策略不存在"))
                .map(rule -> {
                    repository.delete(rule);
                    return true;
                });
    }

    private Either<AbacFailure, Permission> findPermission(String tenant, String code) {
        return permissions.findByTenantAndCode(tenant, code)
                .toEither(() -> new AbacFailure("PERMISSION_NOT_FOUND", "权限不存在"));
    }

    /**
     * 非数字 id 在当前存储中不可能存在；跨租户 id 由 {@code @TenantId} 过滤为空。
     */
    private Option<PolicyRule> findRule(String id) {
        return Try.of(() -> Long.parseLong(id)).toOption().flatMap(repository::findById);
    }

}
