package com.github.zeng.alt.workflow;

import com.github.zeng.alt.tenant.api.TenantMode;
import com.github.zeng.alt.tenant.api.TenantRouting;
import com.github.zeng.alt.tenant.api.TenantRoutingRegistry;
import com.github.zeng.alt.tenant.core.TenantProperties;
import com.github.zeng.alt.tenant.row.DefaultTenantDiscriminatorPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证 workflow 开发环境对 master 租户启用行级隔离。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class WorkflowTenantConfigurationTest {
    @Test
    void masterTenantDoesNotBypassRowIsolation() throws Exception {
        final var environment = new StandardEnvironment();
        new YamlPropertySourceLoader().load("dev", new ClassPathResource("application-dev.yml"))
                .forEach(environment.getPropertySources()::addFirst);
        final TenantProperties properties = Binder.get(environment)
                .bind("alt.tenant", Bindable.of(TenantProperties.class)).get();
        assertThat(properties.getEnabled()).isTrue();
        assertThat(properties.getMode()).isEqualTo(TenantMode.ROW);
        final var routing = mock(TenantRoutingRegistry.class);
        when(routing.resolve("master")).thenReturn(TenantRouting.of("master", TenantMode.ROW, null));

        assertThat(new DefaultTenantDiscriminatorPolicy(properties, routing).isRoot("master")).isFalse();
    }
}
