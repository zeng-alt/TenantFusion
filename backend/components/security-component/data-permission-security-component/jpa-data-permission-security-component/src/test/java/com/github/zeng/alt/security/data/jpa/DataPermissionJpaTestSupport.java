package com.github.zeng.alt.security.data.jpa;

import com.github.zeng.alt.security.data.DataPermissionQuery;
import com.github.zeng.alt.security.data.DataPermissionTemplate;
import com.github.zeng.alt.security.data.core.DataPermissionProperties;
import com.github.zeng.alt.security.data.jpa.fixture.PermissionJpaFixtures;
import io.vavr.control.Either;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Set;
import java.util.UUID;

/**
 * 注解与 JOIN 测试共用的真实 JPA 装配及跨租户样本。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
final class DataPermissionJpaTestSupport {
    private DataPermissionJpaTestSupport() { }

    static ApplicationContextRunner runner() {
        return new ApplicationContextRunner().withAllowBeanDefinitionOverriding(true).withConfiguration(AutoConfigurations.of(
                DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
                DataJpaRepositoriesAutoConfiguration.class, TransactionAutoConfiguration.class,
                DataPermissionJpaAutoConfiguration.class
        )).withUserConfiguration(PermissionJpaFixtures.class)
                .withBean(DataPermissionProperties.class, DataPermissionProperties::new)
                .withBean(DataPermissionTemplate.class, () -> resource -> permission -> () -> Either.right(scope()))
                .withPropertyValues("spring.datasource.url=jdbc:h2:mem:permission_" + UUID.randomUUID(),
                        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.open-in-view=false");
    }

    static DataPermissionQuery scope() {
        return new DataPermissionQuery("t1", "7", false, true, Set.of(20L));
    }

    static void seed(JdbcTemplate jdbc) {
        jdbc.update("""
                insert into permission_document(id, tenant_key, owner_key, department_key, title, enabled) values
                (1, 't1', 7, 10, 'own', true),
                (2, 't1', 8, 20, 'department', true),
                (3, 't1', 9, 30, 'hidden', true),
                (4, 't2', 7, 20, 'foreign', true),
                (5, 't1', 7, 10, 'disabled', false)
                """);
    }
}
