package com.github.zeng.alt.security.core;

import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import com.github.zeng.alt.tenant.api.TenantMetadata;
import com.github.zeng.alt.tenant.api.TenantMetadataProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 校验跨租户切换的身份和目标租户边界。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class TenantSwitchTemplateTest {
    @Test
    void requiresBothConfiguredIdentityAndEnabledTarget() {
        final var properties = new SecurityProperties();
        properties.getAdmin().setId("1");
        properties.getAdmin().setTenant("master");
        final var metadata = mock(TenantMetadataProvider.class);
        @SuppressWarnings("unchecked")
        final ObjectProvider<TenantMetadataProvider> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(metadata);
        when(metadata.findCurrentById("t001")).thenReturn(Optional.of(
                new TenantMetadata("t001", "租户一", null, null, null, null, null, true)));
        final var template = new TenantSwitchTemplate(properties, provider);

        assertThat(template.select(user("1", "master"), "t001").get()).isEqualTo("t001");
        assertThat(template.select(user("1", "t001"), "t001").isLeft()).isTrue();
        assertThat(template.select(user("2", "master"), "t001").isLeft()).isTrue();
        assertThat(template.select(user("1", "master"), "missing").isLeft()).isTrue();
        properties.getAdmin().setEnabled(false);
        assertThat(template.select(user("1", "master"), "t001").isLeft()).isTrue();
    }

    private SecurityUser user(String id, String tenant) {
        return SecurityUser.withUsername("admin").id(id).tenant(tenant).password("unused").roles().build();
    }
}
