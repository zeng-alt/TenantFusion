package com.github.zeng.alt.security.rbac.serve.manager;

import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 Servlet 和 WebFlux 的管理员豁免均受同一开关控制。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AdminAuthorizationManagerTest {
    @ParameterizedTest
    @CsvSource({
            "true, 1, operator, true",
            "false, 1, operator, false",
            "true, 7, superAdmin, true",
            "false, 7, superAdmin, false",
            "true, 7, operator, false"
    })
    void shouldRespectAdminSwitchInBothStacks(boolean enabled, String id, String role, boolean granted) {
        final var properties = new SecurityProperties();
        properties.getAdmin().setEnabled(enabled);
        final var principal = SecurityUser.withUsername("review").id(id).password("unused")
                .roles(new SimpleGrantedAuthority(role)).currentRole(new SimpleGrantedAuthority(role)).build();
        final var authentication = new TestingAuthenticationToken(principal, "unused", role);

        assertThat(new AdminAuthorizationManager(properties).authorize(() -> authentication, null).isGranted())
                .isEqualTo(granted);
        assertThat(new ReactiveAdminAuthorizationManager(properties).authorize(Mono.just(authentication), null)
                .block().isGranted()).isEqualTo(granted);
    }

    @Test
    void shouldDenyMissingAuthenticationInBothStacks() {
        final var properties = new SecurityProperties();
        assertThat(new AdminAuthorizationManager(properties).authorize(() -> null, null).isGranted()).isFalse();
        assertThat(new ReactiveAdminAuthorizationManager(properties).authorize(Mono.empty(), null)
                .block().isGranted()).isFalse();
    }
}
