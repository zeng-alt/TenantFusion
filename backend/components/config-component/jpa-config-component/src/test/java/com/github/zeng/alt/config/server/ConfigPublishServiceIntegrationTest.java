package com.github.zeng.alt.config.server;

import com.github.zeng.alt.config.ConfigTemplate;
import com.github.zeng.alt.config.storage.ConfigStorageTemplate;
import com.github.zeng.alt.config.jpa.mapper.JpaConfigMapper;
import com.github.zeng.alt.config.jpa.JpaConfigAutoConfiguration;
import com.github.zeng.alt.config.client.ConfigClientAutoConfiguration;
import com.github.zeng.alt.config.client.ConfigRemoteTemplate;
import com.github.zeng.alt.config.server.config.ServerConfigAutoConfiguration;
import com.github.zeng.alt.config.server.controller.ConfigClientController;
import com.github.zeng.alt.config.server.controller.ConfigManageController;
import com.github.zeng.alt.config.server.model.ConfigAppCmd;
import com.github.zeng.alt.config.server.model.ConfigCreateCmd;
import com.github.zeng.alt.config.server.model.ConfigPublishCmd;
import com.github.zeng.alt.config.server.model.ConfigRollbackCmd;
import com.github.zeng.alt.config.server.model.ConfigUpdateCmd;
import com.github.zeng.alt.config.server.service.ConfigManageService;
import com.github.zeng.alt.config.server.service.ConfigPublishService;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 发布快照、事务以及同服务双角色集成测试。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class ConfigPublishServiceIntegrationTest {
    @TempDir
    Path directory;

    @Test
    void isolatesDraftsAndRestoresPreviousPublishedSnapshot() {
        runner().run(context -> {
            assertThat(context).hasNotFailed();
            final var manage = context.getBean(ConfigManageService.class);
            final var publish = context.getBean(ConfigPublishService.class);
            manage.createApp(new ConfigAppCmd("shop", "Shop", null)).get();
            final Long id = manage.create(new ConfigCreateCmd("shop", "enabled", "payment", "true", "text", null))
                    .get().configId();
            assertThat(publish.getPublishedConfigs("shop").get()).isEmpty();
            publish.publish(new ConfigPublishCmd("shop", List.of(id), "first")).get();
            manage.update(id, new ConfigUpdateCmd("false", null, null)).get();
            assertThat(publish.getPublishedConfigs("shop").get().getFirst().getContent()).isEqualTo("true");
            publish.publish(new ConfigPublishCmd("shop", List.of(id), "second")).get();
            assertThat(publish.getPublishedConfigs("shop").get().getFirst().getContent()).isEqualTo("false");
            publish.rollback(new ConfigRollbackCmd("shop")).get();
            assertThat(publish.getPublishedConfigs("shop").get().getFirst().getContent()).isEqualTo("true");
            assertThat(context.getBean(ConfigClientController.class).fetchConfigs("shop", null).getData()
                    .getFirst().getContent()).isEqualTo("true");
        });
    }

    @Test
    void commitsLocalUpdatesWithoutHttpAndDoesNotLeakRolledBackTransactions() {
        final ConfigRemoteTemplate remote = mock(ConfigRemoteTemplate.class);
        runner().withConfiguration(AutoConfigurations.of(ConfigClientAutoConfiguration.class))
                .withBean(ConfigRemoteTemplate.class, () -> remote)
                .withPropertyValues(
                    "config.client.app-code=shop",
                    "config.client.retry-delay-ms=60000",
                    "config.client.cache-dir=" + directory
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    final var manage = context.getBean(ConfigManageService.class);
                    final var publish = context.getBean(ConfigPublishService.class);
                    final var config = context.getBean(ConfigTemplate.class).group("payment");
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
                    publish.publish(new ConfigPublishCmd("shop", List.of(id), "first")).get();
                    assertThat(config.opsForValue().getBoolean("enabled").get()).isTrue();
                    manage.update(id, new ConfigUpdateCmd("false", null, null)).get();
                    new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).execute(status -> {
                        publish.publish(new ConfigPublishCmd("shop", List.of(id), "uncommitted")).get();
                        assertThat(config.opsForValue().getBoolean("enabled").get()).isTrue();
                        status.setRollbackOnly();
                        return null;
                    });
                    assertThat(config.opsForValue().getBoolean("enabled").get()).isTrue();
                    publish.publish(new ConfigPublishCmd("shop", List.of(id), "committed")).get();
                    assertThat(config.opsForValue().getBoolean("enabled").get()).isFalse();
                    manage.delete(id).get();
                    assertThat(config.opsForValue().getString("enabled").isDefined()).isTrue();
                    publish.publish(new ConfigPublishCmd("shop", List.of(id), "deleted")).get();
                    assertThat(config.opsForValue().getString("enabled").isEmpty()).isTrue();
                    publish.rollback(new ConfigRollbackCmd("shop")).get();
                    assertThat(config.opsForValue().getBoolean("enabled").get()).isFalse();
                    verifyNoInteractions(remote);
                });
    }

    @Test
    void supportsTheLegacyServiceApiAndRejectsInvalidPublicationSets() {
        runner().run(context -> {
            final var manage = context.getBean(ConfigManageService.class);
            final var publish = context.getBean(ConfigPublishService.class);
            final Long appId = manage.createApp(new ConfigAppCmd("shop", "Shop", null)).get().appId();
            final var config = manage.createConfig(appId, "enabled", null, "true", "text", "first");
            final Long id = config.configId();
            assertThat(manage.getConfig(appId, "enabled", null).content()).isEqualTo("true");
            assertThat(manage.getConfigs(appId, List.of("enabled"))).hasSize(1);
            assertThat(manage.getAllConfigs(appId)).hasSize(1);
            assertThat(context.getBean(ConfigStorageTemplate.class).listHistories(id)).hasSize(1);
            assertThat(manage.createApp(new ConfigAppCmd("shop", null, null)).isLeft()).isTrue();
            assertThat(manage.create(new ConfigCreateCmd("shop", "enabled", null, "x", "text", null)).isLeft()).isTrue();
            assertThat(publish.publish(new ConfigPublishCmd("shop", List.of(Long.MAX_VALUE), null)).isLeft()).isTrue();
            assertThat(publish.rollback(new ConfigRollbackCmd("shop")).isLeft()).isTrue();
            publish.publish(appId, List.of(id), "first");
            manage.updateConfig(id, "false", "second", "operator");
            publish.publish(appId, List.of(id), "second");
            publish.rollback(appId);
            assertThat(publish.getPublishedConfigs("shop").get().getFirst().getContent()).isEqualTo("true");
            manage.deleteConfig(id);
            assertThat(manage.listByAppId(appId).getFirst().status()).isEqualTo("deleted");
        });
    }

    @Test
    void exposesTheCompleteManageAndPublishWorkflow() {
        runner().run(context -> {
            final var admin = context.getBean(ConfigManageController.class);
            assertThat(admin.createApp(new ConfigAppCmd("shop", "Shop", null)).getStatus()).isEqualTo(200);
            assertThat(admin.listApps().getData()).hasSize(1);
            assertThat(admin.getApp("shop").getData().appCode()).isEqualTo("shop");
            final Long id = admin.createConfig(new ConfigCreateCmd("shop", "enabled", null, "true", "text", null))
                    .getData().configId();
            assertThat(admin.listConfigs("shop").getData()).hasSize(1);
            admin.publish(new ConfigPublishCmd("shop", List.of(id), "first"));
            admin.updateConfig(id, new ConfigUpdateCmd("false", null, null));
            admin.publish(new ConfigPublishCmd("shop", List.of(id), "second"));
            assertThat(admin.rollback(new ConfigRollbackCmd("shop")).getStatus()).isEqualTo(200);
            final var client = context.getBean(ConfigClientController.class);
            assertThat(client.fetchConfig("shop", "enabled", "DEFAULT_GROUP").getData().getContent()).isEqualTo("true");
            assertThat(client.fetchConfig("shop", "missing", "DEFAULT_GROUP").getStatus()).isEqualTo(600);
            assertThat(client.fetchConfigs("shop", List.of("missing")).getData()).isEmpty();
            assertThat(admin.deleteConfig(id).getStatus()).isEqualTo(200);
        });
    }

    @Test
    void installsTheSchemaAndValidatesItAgainstTheEntityMappings() {
        runner().withBean(
            DataSource.class,
            () -> {
                final var source = new DriverManagerDataSource(
                    "jdbc:h2:mem:schema_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1",
                    "sa",
                    ""
                );
                new ResourceDatabasePopulator(new ClassPathResource("db/config/schema.sql")).execute(source);
                new ResourceDatabasePopulator(new ClassPathResource("db/config/upgrade-snapshot.sql")).execute(source);
                return source;
            }
        ).withPropertyValues("spring.jpa.hibernate.ddl-auto=validate").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ConfigPublishService.class);
        });
    }

    @Test
    void backsOffWhenTheApplicationProvidesItsOwnStorage() {
        final ConfigStorageTemplate storage = mock(ConfigStorageTemplate.class);
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
            JpaConfigAutoConfiguration.class,
            ServerConfigAutoConfiguration.class
        )).withBean(ConfigStorageTemplate.class, () -> storage).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ConfigStorageTemplate.class);
            assertThat(context.getBean(ConfigStorageTemplate.class)).isSameAs(storage);
            assertThat(context).doesNotHaveBean(JpaConfigMapper.class);
        });
    }

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            DataJpaRepositoriesAutoConfiguration.class,
            TransactionAutoConfiguration.class,
            ServerConfigAutoConfiguration.class,
            JpaConfigAutoConfiguration.class
        )).withPropertyValues(
            "spring.datasource.url=jdbc:h2:mem:config_" + UUID.randomUUID(),
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.open-in-view=false"
        );
    }
}
