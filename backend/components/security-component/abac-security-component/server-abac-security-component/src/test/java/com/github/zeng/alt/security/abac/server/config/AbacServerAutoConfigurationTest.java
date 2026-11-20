package com.github.zeng.alt.security.abac.server.config;

import com.github.zeng.alt.event.EventTemplate;
import com.github.zeng.alt.security.abac.AbacContext;
import com.github.zeng.alt.security.abac.AbacTemplate;
import com.github.zeng.alt.security.abac.client.config.AbacClientAutoConfiguration;
import com.github.zeng.alt.security.abac.core.AbacAutoConfiguration;
import com.github.zeng.alt.security.abac.method.AbacMethodAdvisor;
import com.github.zeng.alt.security.abac.method.AbacMethodAutoConfiguration;
import com.github.zeng.alt.security.abac.policy.AbacPolicyQueryService;
import com.github.zeng.alt.security.abac.server.AbacPolicyService;
import com.github.zeng.alt.security.abac.server.event.AbacPolicyEventListener;
import com.github.zeng.alt.security.abac.server.spi.AbacPolicyRepository;
import com.github.zeng.alt.security.abac.server.spi.AbacPolicyStore;
import com.github.zeng.alt.security.abac.server.web.AbacPolicyController;
import com.github.zeng.alt.security.abac.spel.SpelAbacAutoConfiguration;
import io.vavr.control.Either;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 五模块在服务端独立部署和同进程部署下的实际自动装配回归。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AbacServerAutoConfigurationTest {
    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AbacAutoConfiguration.class, AbacServerAutoConfiguration.class,
                    AbacClientAutoConfiguration.class, SpelAbacAutoConfiguration.class, AbacMethodAutoConfiguration.class))
            .withPropertyValues("security.abac.server.enabled=true", "security.abac.method.enabled=false")
            .withBean(AbacPolicyRepository.class, () -> query -> Either.right(List.of()));

    @Test
    void serverOnlyDoesNotRequireOrCreateClientExecutionBeans() {
        runner.withPropertyValues("security.abac.client.enabled=false").run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(AbacPolicyQueryService.class)
                    .hasSingleBean(AbacPolicyController.class).doesNotHaveBean(AbacTemplate.class)
                    .doesNotHaveBean(AbacMethodAdvisor.class);
        });
    }

    @Test
    void sameProcessSharesTheAuthoritativeQueryAndClientCache() {
        runner.withBean(AbacPolicyStore.class, () -> mock(AbacPolicyStore.class)).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(AbacPolicyQueryService.class)
                    .hasSingleBean(AbacTemplate.class).hasSingleBean(AbacPolicyService.class);
            assertThat(context.getBean(AbacTemplate.class).authorize(
                    new AbacContext("t001", "update", Map.of(), Map.of(), Map.of())).get().get()).isTrue();
        });
    }

    @Test
    void explicitServerDisableDoesNotRequireStorage() {
        runner.withPropertyValues("security.abac.server.enabled=false", "security.abac.client.enabled=false")
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(AbacPolicyQueryService.class));
    }

    @Test
    void remoteNotificationsAreOptInAndReuseExistingEventTemplate() {
        runner.withPropertyValues("security.abac.client.enabled=false", "security.abac.server.notifications-enabled=true")
                .withBean(EventTemplate.class, () -> mock(EventTemplate.class))
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(AbacPolicyEventListener.class));
    }
}
