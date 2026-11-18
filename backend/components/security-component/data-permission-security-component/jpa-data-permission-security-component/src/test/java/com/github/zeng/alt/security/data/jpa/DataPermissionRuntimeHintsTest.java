package com.github.zeng.alt.security.data.jpa;

import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 服务加载器在 AOT 环境中需要的构造器与服务文件提示。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class DataPermissionRuntimeHintsTest {
    @Test
    void registersHibernateServiceConstructorsAndDescriptors() throws Exception {
        final RuntimeHints hints = new RuntimeHints();
        new DataPermissionRuntimeHints().registerHints(hints, getClass().getClassLoader());
        for (Class<?> service : new Class<?>[] {DataPermissionMappingContributor.class,
                DataPermissionLoadListener.class, DataPermissionServiceContributor.class}) {
            assertThat(RuntimeHintsPredicates.reflection().onConstructor(service.getDeclaredConstructor())
                    .test(hints)).isTrue();
        }
        assertThat(RuntimeHintsPredicates.resource().forResource(
                "META-INF/services/org.hibernate.boot.spi.AdditionalMappingContributor").test(hints)).isTrue();
    }
}
