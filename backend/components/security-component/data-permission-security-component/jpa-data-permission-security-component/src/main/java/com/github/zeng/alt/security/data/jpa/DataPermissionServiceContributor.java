package com.github.zeng.alt.security.data.jpa;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.query.spi.NativeQueryInterpreter;
import org.hibernate.service.spi.ServiceContributor;

/**
 * 注册工厂独立的原生查询执行守卫，不维护全局 SessionFactory 状态。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class DataPermissionServiceContributor implements ServiceContributor {
    @Override
    public void contribute(StandardServiceRegistryBuilder builder) {
        final boolean ignore = Boolean.parseBoolean(String.valueOf(
                builder.getSettings().getOrDefault("hibernate.query.native.ignore_jdbc_parameters", false)));
        builder.addService(NativeQueryInterpreter.class, new DataPermissionNativeQueryInterpreter(ignore));
    }
}
