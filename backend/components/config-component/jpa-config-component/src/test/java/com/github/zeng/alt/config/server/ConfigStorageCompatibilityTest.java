package com.github.zeng.alt.config.server;

import com.github.zeng.alt.config.jdbc.JdbcConfigAutoConfiguration;
import com.github.zeng.alt.config.jpa.JpaConfigAutoConfiguration;
import com.github.zeng.alt.config.jpa.JpaConfigStorageTemplate;
import com.github.zeng.alt.config.server.config.ServerConfigAutoConfiguration;
import com.github.zeng.alt.config.server.model.ConfigAppCmd;
import com.github.zeng.alt.config.server.model.ConfigCreateCmd;
import com.github.zeng.alt.config.server.model.ConfigPublishCmd;
import com.github.zeng.alt.config.server.model.ConfigRollbackCmd;
import com.github.zeng.alt.config.server.service.ConfigManageService;
import com.github.zeng.alt.config.server.service.ConfigPublishService;
import com.github.zeng.alt.config.storage.ConfigStorageTemplate;
import com.github.zeng.alt.config.storage.model.ConfigEntryDto;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * JPA / JDBC 的表结构兼容和装配优先级测试。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class ConfigStorageCompatibilityTest {
    @Test
    void readsAndUpdatesTheSameConfigurationWithEitherAdapter() {
        runner().run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ConfigStorageTemplate.class);
            final ConfigStorageTemplate jpa = context.getBean(ConfigStorageTemplate.class);
            assertThat(jpa).isInstanceOf(JpaConfigStorageTemplate.class);
            final ConfigStorageTemplate jdbc = new JdbcConfigAutoConfiguration().configStorageTemplate(
                context.getBean(DataSource.class),
                context.getBean(PlatformTransactionManager.class)
            );
            final var manage = context.getBean(ConfigManageService.class);
            final var publish = context.getBean(ConfigPublishService.class);
            final Long appId = manage.createApp(new ConfigAppCmd("shop", "Shop", null)).get().appId();
            final Long id = manage.create(new ConfigCreateCmd("shop", "enabled", "payment", "true", "text", null))
                    .get().configId();
            publish.publish(new ConfigPublishCmd("shop", List.of(id), "JPA publication")).get();
            assertThat(jdbc.getApp(appId)).isEqualTo(jpa.getApp(appId));
            assertThat(jdbc.getConfig(id)).isEqualTo(jpa.getConfig(id));
            final ConfigEntryDto saved = jdbc.execute(() -> jdbc.updateConfig(jdbc.getConfig(id).get().toBuilder()
                    .content("false").status("draft").build()));
            assertThat(jpa.getConfig(id).get()).isEqualTo(saved);
            publish.publish(new ConfigPublishCmd("shop", List.of(id), "publication after JDBC update")).get();
            assertThat(jdbc.getLatestRelease(appId).get().snapshot()).contains("false");
            publish.rollback(new ConfigRollbackCmd("shop")).get();
            assertThat(jdbc.getLatestRelease(appId).get().snapshot()).contains("true");
        });
    }

    @Test
    void givesCustomStoragePrecedenceOverBothAdapters() {
        final ConfigStorageTemplate storage = mock(ConfigStorageTemplate.class);
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
            JdbcConfigAutoConfiguration.class,
            JpaConfigAutoConfiguration.class,
            ServerConfigAutoConfiguration.class
        )).withBean(ConfigStorageTemplate.class, () -> storage).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ConfigStorageTemplate.class);
            assertThat(context.getBean(ConfigStorageTemplate.class)).isSameAs(storage);
            assertThat(context).doesNotHaveBean(DataSource.class);
        });
    }

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            DataJpaRepositoriesAutoConfiguration.class,
            TransactionAutoConfiguration.class,
            JpaConfigAutoConfiguration.class,
            JdbcConfigAutoConfiguration.class,
            ServerConfigAutoConfiguration.class
        )).withPropertyValues(
            "spring.datasource.url=jdbc:h2:mem:config_compat_" + UUID.randomUUID(),
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.open-in-view=false"
        );
    }
}
