package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.admin.infrastructure.repository.PolicyRuleRepository;
import com.github.zeng.alt.security.abac.AbacContext;
import com.github.zeng.alt.security.abac.AbacPolicy;
import com.github.zeng.alt.security.abac.core.AbacPolicyLoader;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
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
public class AbacPolicyLoaderImpl implements AbacPolicyLoader {

    private final PolicyRuleRepository repository;
    private final AbacPolicyMapper mapper;

    /**
     * @throws IllegalStateException 授权上下文的租户与当前持久化租户不一致
     */
    @Override
    public List<AbacPolicy> list(AbacContext context) {
        if (!context.tenant().equals(TenantContextHolder.getTenantId())) {
            throw new IllegalStateException("ABAC context tenant does not match the current tenant");
        }
        return repository.listByPermission(context.tenant(), context.permission()).stream()
                .map(mapper::toPolicy)
                .toList();
    }
}
