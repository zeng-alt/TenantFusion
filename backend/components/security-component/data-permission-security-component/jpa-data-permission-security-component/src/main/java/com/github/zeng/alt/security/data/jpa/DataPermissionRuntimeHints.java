package com.github.zeng.alt.security.data.jpa;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * 为 Hibernate 服务加载器注册组件构造器及服务声明。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class DataPermissionRuntimeHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        for (Class<?> service : new Class<?>[] {DataPermissionMappingContributor.class,
                DataPermissionLoadListener.class, DataPermissionServiceContributor.class}) {
            hints.reflection().registerType(service, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
        }
        hints.resources().registerPattern("META-INF/services/org.hibernate.boot.spi.AdditionalMappingContributor");
        hints.resources().registerPattern("META-INF/services/org.hibernate.integrator.spi.Integrator");
        hints.resources().registerPattern("META-INF/services/org.hibernate.service.spi.ServiceContributor");
    }
}
