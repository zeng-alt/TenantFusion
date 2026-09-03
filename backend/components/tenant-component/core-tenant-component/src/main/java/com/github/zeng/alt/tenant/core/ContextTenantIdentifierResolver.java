package com.github.zeng.alt.tenant.core;

import com.github.zeng.alt.tenant.api.TenantContextHolder;
import com.github.zeng.alt.tenant.api.TenantDiscriminatorPolicy;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Hibernate 唯一的租户标识解析器（{@code hibernate.tenant_identifier_resolver}）。
 * <p>
 * 所有隔离档位都依赖它：连接层用它选数据源 / schema，行级用它填判别列。
 * 因此它必须放在 core 而非某个策略模块。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
public class ContextTenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

    private final TenantProperties properties;
    private final ObjectProvider<TenantDiscriminatorPolicy> discriminatorPolicy;

    public ContextTenantIdentifierResolver(
            TenantProperties properties,
            ObjectProvider<TenantDiscriminatorPolicy> discriminatorPolicy) {
        this.properties = properties;
        this.discriminatorPolicy = discriminatorPolicy;
    }

    /**
     * 绝不能返回 null——Hibernate 在启用多租户后会以此为 Session 的键。
     */
    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenantId = TenantContextHolder.getTenantId();
        return tenantId == null || tenantId.isBlank()
                ? properties.getDefaultTenantId()
                : tenantId;
    }

    /**
     * 返回 false，允许在同一个 Session 生命周期内不校验租户变化。
     * <p>
     * 置为 true 会让 Hibernate 在租户标识与已存在 Session 不一致时抛错；本组件在过滤器里
     * 于 Session 打开前就固定了租户，无需这层校验，且开启后会影响事务嵌套场景。
     */
    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    /**
     * 委派给 {@link TenantDiscriminatorPolicy}：返回 true 时 Hibernate 不施加
     * {@code @TenantId} 判别条件。没有 row 模块时始终返回 true，即完全不做行级过滤。
     */
    @Override
    public boolean isRoot(String tenantId) {
        TenantDiscriminatorPolicy policy = discriminatorPolicy.getIfAvailable();
        return policy == null || policy.isRoot(tenantId);
    }
}
