package com.github.zeng.alt.tenant.row;

import com.github.zeng.alt.tenant.api.TenantDiscriminatorPolicy;
import com.github.zeng.alt.tenant.api.TenantRouting;
import com.github.zeng.alt.tenant.api.TenantRoutingRegistry;
import com.github.zeng.alt.tenant.core.TenantProperties;

/**
 * 行级判别条件的生效策略。
 * <p>
 * 两种情况绕过判别条件（{@code isRoot} 返回 true）：
 * <ol>
 *   <li>请求方是超管租户（{@code alt.tenant.root-tenant-id}），需要跨租户视图</li>
 *   <li>该租户的路由未开启行级隔离——混合部署下走独立库/独立 schema 的租户本就不需要判别列，
 *       再加一句 {@code where tenant_by = ?} 只会让它查不到从别处导入的数据</li>
 * </ol>
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
public class DefaultTenantDiscriminatorPolicy implements TenantDiscriminatorPolicy {

    private final TenantProperties properties;
    private final TenantRoutingRegistry registry;

    public DefaultTenantDiscriminatorPolicy(
            TenantProperties properties, TenantRoutingRegistry registry) {
        this.properties = properties;
        this.registry = registry;
    }

    @Override
    public boolean isRoot(String tenantId) {
        String rootTenantId = properties.getRootTenantId();
        if (rootTenantId != null && rootTenantId.equals(tenantId)) {
            return true;
        }
        TenantRouting routing = registry.resolve(tenantId);
        return !routing.rowIsolated();
    }
}
