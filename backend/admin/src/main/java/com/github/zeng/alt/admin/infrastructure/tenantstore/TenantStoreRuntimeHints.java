package com.github.zeng.alt.admin.infrastructure.tenantstore;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * 注册自建 Liquibase 执行器使用的独立租户库迁移资源。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public class TenantStoreRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources().registerPattern("db/changelog/db.changelog-tenant.yaml")
                .registerPattern("db/changelog/tenant/**");
    }
}
