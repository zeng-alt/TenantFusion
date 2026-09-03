package com.github.zeng.alt.tenant.api;

import org.springframework.core.Ordered;

/**
 * SQL 文本重写 SPI，表级隔离由此实现。
 * <p>
 * Hibernate 的 {@code StatementInspector} 只能注册一个且只拿到 SQL 字符串，
 * 所以 core 注册一个组合实现，再串联本接口的多个实现。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
public interface TenantSqlRewriter extends Ordered {

    /**
     * 本重写器是否适用于该路由。
     *
     * @param routing 租户路由
     * @return true 表示需要重写
     */
    boolean supports(TenantRouting routing);

    /**
     * 重写 SQL。实现必须是幂等的，且在无需改写时原样返回入参。
     *
     * @param sql     Hibernate 生成的 SQL
     * @param routing 租户路由
     * @return 重写后的 SQL
     */
    String rewrite(String sql, TenantRouting routing);

    @Override
    default int getOrder() {
        return 0;
    }
}
