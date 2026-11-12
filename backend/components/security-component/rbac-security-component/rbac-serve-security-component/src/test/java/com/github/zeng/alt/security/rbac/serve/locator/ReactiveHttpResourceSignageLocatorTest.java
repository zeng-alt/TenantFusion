package com.github.zeng.alt.security.rbac.serve.locator;

import com.github.zeng.alt.security.api.HttpResource;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.rbac.serve.repository.DefaultRbacResourceService;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceLoader;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceService;
import com.github.zeng.alt.storage.CacheStringOperations;
import com.github.zeng.alt.storage.StorageTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 Reactive 资源权限定位使用主体租户，并隔离默认服务的缓存键。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class ReactiveHttpResourceSignageLocatorTest {
    private static final HttpResource RESOURCE = HttpResource.of("/records", "GET");

    @Test
    void shouldReadTenantFromAuthenticationPrincipal() {
        final var service = mock(RbacResourceService.class);

        new ReactiveHttpResourceSignageLocator(service).load(RESOURCE, authentication("t001")).block();

        verify(service).findPermission("t001", RESOURCE);
    }

    @Test
    void shouldKeepTwoTenantsPermissionMappingsInSeparateCacheEntries() {
        final Map<String, Object> entries = new HashMap<>();
        final var strings = mock(CacheStringOperations.class);
        when(strings.get(anyString(), any())).thenAnswer(call -> entries.get(call.getArgument(0)));
        doAnswer(call -> {
            entries.put(call.getArgument(0), call.getArgument(1));
            return null;
        }).when(strings).set(anyString(), any(), any(Duration.class));
        final var storage = mock(StorageTemplate.class);
        when(storage.opsForString()).thenReturn(strings);
        final var loader = mock(RbacResourceLoader.class);
        when(loader.loadPermissionByResource(anyString(), eq(RESOURCE)))
                .thenAnswer(call -> call.<String>getArgument(0) + ":read");
        final var locator = new ReactiveHttpResourceSignageLocator(new DefaultRbacResourceService(storage, loader));

        assertThat(locator.load(RESOURCE, authentication("t001")).block()).isEqualTo("t001:read");
        assertThat(locator.load(RESOURCE, authentication("t002")).block()).isEqualTo("t002:read");
        assertThat(entries).containsKeys("rbac:permission:t001:" + RESOURCE.getKey(),
                "rbac:permission:t002:" + RESOURCE.getKey());
        verify(loader).loadPermissionByResource("t001", RESOURCE);
        verify(loader).loadPermissionByResource("t002", RESOURCE);
    }

    private Authentication authentication(String tenant) {
        final var user = SecurityUser.withUsername("review").id("7").tenant(tenant).password("unused").roles().build();
        return new TestingAuthenticationToken(user, "unused", "operator");
    }
}
