package com.github.zeng.alt.tenant.liquibase;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.MemberCategory;

import java.sql.Connection;

/**
 * 注册迁移连接归还时恢复 schema 所使用的 JDBC 代理。
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class TenantLiquibaseRuntimeHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.proxies().registerJdkProxy(Connection.class);
        hints.reflection().registerType(Connection.class, MemberCategory.INVOKE_PUBLIC_METHODS);
    }
}
