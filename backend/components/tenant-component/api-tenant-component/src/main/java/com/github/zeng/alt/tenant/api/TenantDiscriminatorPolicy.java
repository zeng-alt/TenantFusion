package com.github.zeng.alt.tenant.api;

/**
 * 行级判别列的生效策略。
 * <p>
 * 对应 Hibernate 的 {@code CurrentTenantIdentifierResolver#isRoot(Object)}：返回 true 时
 * Hibernate 不对该租户施加 {@code @TenantId} 判别条件。用于两类场景——超管跨租户视图，
 * 以及混合部署下走独立库/独立 schema、本就不需要判别列的租户。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
public interface TenantDiscriminatorPolicy {

    /**
     * 该租户是否应绕过判别条件。
     *
     * @param tenantId 租户标识
     * @return true 表示绕过
     */
    boolean isRoot(String tenantId);
}
