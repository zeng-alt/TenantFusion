package com.github.zeng.alt.config.jdbc;

import com.github.zeng.alt.config.ConfigException;
import com.github.zeng.alt.config.ConfigTemplate;
import com.github.zeng.alt.config.client.ConfigClientAutoConfiguration;
import com.github.zeng.alt.config.client.ConfigRemoteTemplate;
import com.github.zeng.alt.config.server.config.ServerConfigAutoConfiguration;
import com.github.zeng.alt.config.server.model.ConfigAppCmd;
import com.github.zeng.alt.config.server.model.ConfigCreateCmd;
import com.github.zeng.alt.config.server.model.ConfigPublishCmd;
import com.github.zeng.alt.config.server.model.ConfigRollbackCmd;
import com.github.zeng.alt.config.server.model.ConfigUpdateCmd;
import com.github.zeng.alt.config.server.service.ConfigManageService;
import com.github.zeng.alt.config.server.service.ConfigPublishService;
import com.github.zeng.alt.config.storage.ConfigStorageTemplate;
import com.github.zeng.alt.config.storage.model.ConfigAppDto;
import com.github.zeng.alt.config.storage.model.ConfigEntryDto;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * JDBC 存储、版本更新及发布事务集成测试。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class JdbcConfigStorageTemplateTest {
    @TempDir
    Path directory;

    @Test
    void providesCrudWithoutJpa() {
        assertThat(ClassUtils.isPresent("jakarta.persistence.Entity", getClass().getClassLoader())).isFalse();
        runner().run(context -> {
            assertThat(context).hasNotFailed();
            final ConfigStorageTemplate storage = context.getBean(ConfigStorageTemplate.class);
            final ConfigAppDto app = storage.createApp(ConfigAppDto.builder().appCode("shop").appName("Shop").build());
            final ConfigEntryDto first = storage.createConfig(ConfigEntryDto.builder().appId(app.appId())
                    .dataId("enabled").group("payment").content("true").build());
            final ConfigEntryDto other = storage.createConfig(ConfigEntryDto.builder().appId(app.appId())
                    .dataId("enabled").group("delivery").content("false").build());
            assertThat(storage.getApp(app.appCode()).get()).isEqualTo(app);
            assertThat(storage.getApp(app.appId()).get()).isEqualTo(app);
            assertThat(storage.listApps()).containsExactly(app);
            assertThat(app.createdDate()).isNotNull();
            assertThat(storage.getConfig(app.appId(), "enabled", "payment").get()).isEqualTo(first);
            assertThat(storage.listConfigs(app.appId())).hasSize(2);
            assertThat(storage.listConfigs(List.of(first.configId()))).containsExactly(first);
            assertThat(storage.listConfigs(List.of())).isEmpty();
            final ConfigEntryDto updated = storage.updateConfig(first.toBuilder().content("false").build());
            assertThat(updated.version()).isEqualTo(first.version() + 1);
            assertThat(updated.content()).isEqualTo("false");
            assertThatThrownBy(() -> storage.updateConfig(first)).isInstanceOf(OptimisticLockingFailureException.class);
            assertThat(storage.deleteConfig(first.configId()).get().status()).isEqualTo("deleted");
            assertThat(storage.getConfig(other.configId()).get().status()).isEqualTo("draft");
            assertThat(storage.getConfig(Long.MAX_VALUE).isEmpty()).isTrue();
            assertThat(storage.getApp(Long.MAX_VALUE).isEmpty()).isTrue();
            assertThat(storage.deleteConfig(Long.MAX_VALUE).isEmpty()).isTrue();
        });
    }

    @Test
    void publishesRollsBackAndUpdatesTheLocalClientAfterCommit() {
        final ConfigRemoteTemplate remote = mock(ConfigRemoteTemplate.class);
        runner().withConfiguration(AutoConfigurations.of(ConfigClientAutoConfiguration.class))
                .withBean(ConfigRemoteTemplate.class, () -> remote)
                .withPropertyValues("config.client.app-code=shop", "config.client.cache-dir=" + directory)
                .run(context -> {
                    final var manage = context.getBean(ConfigManageService.class);
                    final var publish = context.getBean(ConfigPublishService.class);
                    final var config = context.getBean(ConfigTemplate.class).group("payment");
                    final ConfigStorageTemplate storage = context.getBean(ConfigStorageTemplate.class);
                    manage.createApp(new ConfigAppCmd("shop", "Shop", null)).get();
                    final Long id = manage.create(new ConfigCreateCmd(
                        "shop",
                        "enabled",
                        "payment",
                        "true",
                        "text",
                        null
                    ))
                            .get().configId();
                    assertThat(publish.getPublishedConfigs("shop").get()).isEmpty();
                    publish.publish(new ConfigPublishCmd("shop", List.of(id), "first")).get();
                    manage.update(id, new ConfigUpdateCmd("false", null, "operator")).get();
                    assertThat(config.opsForValue().getBoolean("enabled").get()).isTrue();
                    new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).execute(status -> {
                        publish.publish(new ConfigPublishCmd("shop", List.of(id), "rolled back transaction")).get();
                        assertThat(config.opsForValue().getBoolean("enabled").get()).isTrue();
                        status.setRollbackOnly();
                        return null;
                    });
                    assertThat(config.opsForValue().getBoolean("enabled").get()).isTrue();
                    publish.publish(new ConfigPublishCmd("shop", List.of(id), "second")).get();
                    assertThat(config.opsForValue().getBoolean("enabled").get()).isFalse();
                    publish.rollback(new ConfigRollbackCmd("shop")).get();
                    assertThat(config.opsForValue().getBoolean("enabled").get()).isTrue();
                    manage.delete(id).get();
                    publish.publish(new ConfigPublishCmd("shop", List.of(id), "delete")).get();
                    assertThat(config.opsForValue().getString("enabled").isEmpty()).isTrue();
                    assertThat(storage.listHistories(id)).hasSize(3);
                    verifyNoInteractions(remote);
                });
    }

    @Test
    void rollsBackAllStorageWritesWhenAnOperationFails() {
        runner().run(context -> {
            final ConfigStorageTemplate storage = context.getBean(ConfigStorageTemplate.class);
            assertThatThrownBy(() -> storage.execute(() -> {
                storage.createApp(ConfigAppDto.builder().appCode("rolled-back").build());
                throw new ConfigException("storage failure");
            })).isInstanceOf(ConfigException.class);
            assertThat(storage.getApp("rolled-back").isEmpty()).isTrue();
        });
    }

    @Test
    void backsOffForACustomStorage() {
        final ConfigStorageTemplate custom = mock(ConfigStorageTemplate.class);
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
            JdbcConfigAutoConfiguration.class,
            ServerConfigAutoConfiguration.class
        )).withBean(ConfigStorageTemplate.class, () -> custom).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(ConfigStorageTemplate.class)).isSameAs(custom);
            assertThat(context).doesNotHaveBean(DataSource.class);
        });
    }

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            TransactionAutoConfiguration.class,
            JdbcConfigAutoConfiguration.class,
            ServerConfigAutoConfiguration.class
        )).withBean(
            DataSource.class,
            () -> {
                final var source = new DriverManagerDataSource(
                    "jdbc:h2:mem:config_jdbc_" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                    "sa",
                    ""
                );
                new ResourceDatabasePopulator(new ClassPathResource("db/config/schema.sql")).execute(source);
                return source;
            }
        );
    }
}
