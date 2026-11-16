package com.github.zeng.alt.config.server;

import com.github.zeng.alt.config.ConfigException;
import com.github.zeng.alt.config.ConfigTemplate;
import com.github.zeng.alt.config.annotation.ConfigProperties;
import lombok.Data;
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
import com.github.zeng.alt.config.storage.ConfigStorageTemplate;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 自定义存储的 CRUD、发布、回滚及同进程客户端集成测试。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class ConfigStorageIntegrationTest {
    @TempDir
    Path directory;

    @Test
    void supportsCustomStorageWithoutJpaOrADataSource() {
        assertThat(ClassUtils.isPresent("jakarta.persistence.Entity", getClass().getClassLoader())).isFalse();
        runner(new MemoryConfigStorageTemplate()).run(context -> {
            assertThat(context).hasNotFailed();
            final var admin = context.getBean(ConfigManageController.class);
            final Long appId = admin.createApp(new ConfigAppCmd("shop", "Shop", null)).getData().appId();
            assertThat(admin.listApps().getData()).hasSize(1);
            assertThat(admin.getApp("shop").getData().appId()).isEqualTo(appId);
            final Long id = admin.createConfig(new ConfigCreateCmd("shop", "enabled", null, "true", "text", null))
                    .getData().configId();
            assertThat(admin.listConfigs("shop").getData()).hasSize(1);
            admin.publish(new ConfigPublishCmd("shop", List.of(id), "first"));
            admin.updateConfig(id, new ConfigUpdateCmd("false", null, null));
            final var client = context.getBean(ConfigClientController.class);
            assertThat(client.fetchConfig("shop", "enabled", "DEFAULT_GROUP").getData().getContent()).isEqualTo("true");
            admin.publish(new ConfigPublishCmd("shop", List.of(id), "second"));
            assertThat(client.fetchConfigs("shop", null).getData().getFirst().getContent()).isEqualTo("false");
            assertThat(admin.rollback(new ConfigRollbackCmd("shop")).getStatus()).isEqualTo(200);
            assertThat(client.fetchConfigs("shop", null).getData().getFirst().getContent()).isEqualTo("true");
            admin.deleteConfig(id);
            admin.publish(new ConfigPublishCmd("shop", List.of(id), "deleted"));
            assertThat(client.fetchConfigs("shop", null).getData()).isEmpty();
        });
    }

    @Test
    void notifiesTheLocalClientOnlyAfterSuccessfulStorageExecution() {
        final var storage = spy(new MemoryConfigStorageTemplate());
        final var remote = mock(ConfigRemoteTemplate.class);
        runner(storage).withConfiguration(AutoConfigurations.of(ConfigClientAutoConfiguration.class))
                .withUserConfiguration(PaymentSettings.class)
                .withBean(ConfigRemoteTemplate.class, () -> remote)
                .withPropertyValues("config.client.app-code=shop", "config.client.cache-dir=" + directory)
                .run(context -> {
                    final var manage = context.getBean(ConfigManageService.class);
                    final var publish = context.getBean(ConfigPublishService.class);
                    final var config = context.getBean(ConfigTemplate.class);
                    manage.createApp(new ConfigAppCmd("shop", null, null)).get();
                    final Long id = manage.create(new ConfigCreateCmd(
                        "shop",
                        "payment.enabled",
                        null,
                        "true",
                        "text",
                        null
                    ))
                            .get().configId();
                    publish.publish(new ConfigPublishCmd("shop", List.of(id), "first")).get();
                    assertThat(context.getBean(PaymentSettings.class).isEnabled()).isTrue();
                    manage.update(id, new ConfigUpdateCmd("false", null, null)).get();
                    doThrow(new ConfigException("storage failed")).when(storage).createRelease(any());
                    assertThatThrownBy(() -> publish.publish(new ConfigPublishCmd("shop", List.of(id), "failed")))
                            .isInstanceOf(ConfigException.class);
                    assertThat(config.opsForValue().getBoolean("payment.enabled").get()).isTrue();
                    assertThat(context.getBean(PaymentSettings.class).isEnabled()).isTrue();
                    assertThat(storage.getConfig(id).get().status()).isEqualTo("draft");
                    assertThat(storage.listHistories(id)).hasSize(2);
                    verifyNoInteractions(remote);
                });
    }

    @Test
    void preservesLegacyReadsAndReportsExpectedBusinessFailures() {
        runner(new MemoryConfigStorageTemplate()).run(context -> {
            final var manage = context.getBean(ConfigManageService.class);
            final var publish = context.getBean(ConfigPublishService.class);
            final Long appId = manage.createApp(new ConfigAppCmd("shop", null, null)).get().appId();
            final Long id = manage.createConfig(appId, "enabled", null, "true", "text", null).configId();
            assertThat(manage.getConfig(appId, "enabled", null).content()).isEqualTo("true");
            assertThat(manage.getConfigs(appId, List.of("enabled"))).hasSize(1);
            assertThat(manage.getAllConfigs(appId)).hasSize(1);
            assertThat(manage.createApp(new ConfigAppCmd("shop", null, null)).isLeft()).isTrue();
            assertThat(manage.create(new ConfigCreateCmd("shop", "enabled", null, "x", "text", null)).isLeft()).isTrue();
            assertThat(publish.publish(new ConfigPublishCmd("shop", List.of(Long.MAX_VALUE), null)).isLeft()).isTrue();
            assertThat(publish.rollback(new ConfigRollbackCmd("shop")).isLeft()).isTrue();
            publish.publish(appId, List.of(id), "first");
            manage.updateConfig(id, "false", null, "operator");
            publish.publish(appId, List.of(id), "second");
            publish.rollback(appId);
            manage.deleteConfig(id);
            assertThat(manage.listByAppId(appId).getFirst().status()).isEqualTo("deleted");
        });
    }

    /**
     * 自定义存储和客户端共用的配置类。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    @Data
    @ConfigProperties(prefix = "payment")
    public static class PaymentSettings {
        private boolean enabled;
    }

    private ApplicationContextRunner runner(ConfigStorageTemplate storage) {
        return new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(ServerConfigAutoConfiguration.class))
                .withBean(ConfigStorageTemplate.class, () -> storage);
    }
}
