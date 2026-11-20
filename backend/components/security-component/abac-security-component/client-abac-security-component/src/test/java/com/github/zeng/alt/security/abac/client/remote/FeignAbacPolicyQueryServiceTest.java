package com.github.zeng.alt.security.abac.client.remote;

import com.github.zeng.alt.security.abac.client.config.AbacClientProperties;
import com.github.zeng.alt.security.abac.policy.AbacPolicyQuery;
import com.github.zeng.alt.security.abac.policy.AbacPolicyQueryService;
import com.github.zeng.alt.security.abac.policy.AbacPolicySnapshotDto;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Feign 适配及客户端独立身份配置，不读用户请求或用户 Token。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class FeignAbacPolicyQueryServiceTest {
    private final AbacPolicyFeignClient client = mock(AbacPolicyFeignClient.class);
    private final FeignAbacPolicyQueryService source = new FeignAbacPolicyQueryService(client);
    private final AbacPolicyQuery query = new AbacPolicyQuery("t001", "update");

    @Test
    void readsPolicySnapshotUsingTheQueryTenantForBothProtocolAndHeader() {
        final var snapshot = new AbacPolicySnapshotDto("t001", "update", "v1", List.of());
        when(client.get("t001", "update", "t001")).thenReturn(snapshot);
        assertThat(source.get(query).get()).isSameAs(snapshot);
        verify(client).get("t001", "update", "t001");
    }

    @Test
    void emptyOrFailedFeignResponsesAreNotEmptyRuleSets() {
        assertThat(source.get(query).getLeft().code()).isEqualTo("POLICY_REMOTE_EMPTY");
        when(client.get("t001", "update", "t001")).thenThrow(new IllegalStateException("unavailable"));
        assertThat(source.get(query).getLeft().code()).isEqualTo("POLICY_REMOTE_FAILED");
    }

    @Test
    void serviceInterceptorRemovesUserAuthorizationAndAddsOnlyServiceCredentials() {
        final AbacClientProperties properties = properties();
        final var request = new RequestTemplate();
        request.header("Authorization", "Bearer unrelated-user");
        new AbacServiceRequestInterceptor(properties).apply(request);
        assertThat(request.headers()).doesNotContainKey("Authorization");
        assertThat(request.headers().get(AbacPolicyQueryService.CLIENT_HEADER)).containsExactly("workflow");
        assertThat(request.headers().get(AbacPolicyQueryService.KEY_HEADER)).containsExactly("test-key");
    }

    @Test
    void feignConfigurationIsIsolatedAndRequiresServiceCredentials() {
        final var configuration = new AbacFeignConfiguration();
        assertThat(configuration.abacFeignClientConfigurer().inheritParentConfiguration()).isFalse();
        assertThat(configuration.abacRequestOptions(properties()).connectTimeoutMillis()).isEqualTo(3000);
        assertThat(configuration.abacRequestOptions(properties()).readTimeoutMillis()).isEqualTo(5000);
        assertThat(configuration.abacRequestOptions(properties()).isFollowRedirects()).isFalse();
        assertThat(configuration.abacServiceRequestInterceptor(properties())).isInstanceOf(AbacServiceRequestInterceptor.class);
        assertThatThrownBy(() -> configuration.abacServiceRequestInterceptor(new AbacClientProperties()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("client-id");
        final var missingKey = properties();
        missingKey.setServiceKey("");
        assertThatThrownBy(() -> configuration.abacServiceRequestInterceptor(missingKey))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("service-key");
    }

    private AbacClientProperties properties() {
        final var properties = new AbacClientProperties();
        properties.setClientId("workflow");
        properties.setServiceKey("test-key");
        return properties;
    }
}
