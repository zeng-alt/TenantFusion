package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.admin.infrastructure.repository.PolicyRuleRepository;
import com.github.zeng.alt.security.abac.policy.AbacPolicyQuery;
import com.github.zeng.alt.security.abac.AbacPolicy;
import com.github.zeng.alt.security.abac.AbacFailure;
import com.github.zeng.alt.security.abac.server.spi.AbacPolicyRepository;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import io.vavr.control.Either;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 在当前租户边界内加载权限策略。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Component
@RequiredArgsConstructor
public class JpaAbacPolicyRepository implements AbacPolicyRepository {

    private final PolicyRuleRepository repository;
    private final AbacPolicyMapper mapper;

    @Override
    public Either<AbacFailure, List<AbacPolicy>> list(AbacPolicyQuery query) {
        if (!query.tenant().equals(TenantContextHolder.getTenantId())) {
            return Either.left(new AbacFailure("TENANT_MISMATCH", "策略租户与当前数据租户不一致"));
        }
        return Try.of(() -> repository.listByPermission(query.tenant(), query.permission()).stream()
                .map(mapper::toPolicy)
                .toList()).toEither(new AbacFailure("POLICY_QUERY_FAILED", "策略数据库查询失败"));
    }
}
