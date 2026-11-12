package com.github.zeng.alt.security.rbac.serve.repository;

import com.github.zeng.alt.storage.CacheStringOperations;
import com.github.zeng.alt.storage.StorageTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证多角色权限缓存的部分命中、空权限命中及所属角色过滤。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class DefaultRbacResourceServiceTest {
    private final Map<String, Object> entries = new HashMap<>();
    private final RbacResourceLoader loader = mock(RbacResourceLoader.class);
    private DefaultRbacResourceService service;

    @BeforeEach
    void setUp() {
        final var strings = mock(CacheStringOperations.class);
        when(strings.get(anyString(), any())).thenAnswer(call -> entries.get(call.getArgument(0)));
        doAnswer(call -> {
            entries.put(call.getArgument(0), call.getArgument(1));
            return null;
        }).when(strings).set(anyString(), any(), any(Duration.class));
        final var storage = mock(StorageTemplate.class);
        when(storage.opsForString()).thenReturn(strings);
        service = new DefaultRbacResourceService(storage, loader);
        entries.put("rbac:user:role:t001:7", Set.of("A", "B"));
    }

    @Test
    void shouldLoadOnlyMissingRolesAndMergeCachedPermissions() {
        entries.put("rbac:role:permissions:t001:A", Set.of("a:read"));
        when(loader.loadPermissions(List.of("B"), "t001")).thenReturn(Map.of("B", Set.of("b:write")));

        assertThat(service.findRolePermission(List.of("A", "B"), "7", "t001"))
                .containsExactlyInAnyOrder("a:read", "b:write");
        verify(loader).loadPermissions(List.of("B"), "t001");
        assertThat(entries).containsEntry("rbac:role:permissions:t001:B", Set.of("b:write"));
    }

    @Test
    void shouldTreatCachedEmptyPermissionsAsAHit() {
        entries.put("rbac:role:permissions:t001:A", Set.of());

        assertThat(service.findRolePermission(List.of("A"), "7", "t001")).isEmpty();
        verify(loader, never()).loadPermissions(any(), any());
    }

    @Test
    void shouldUseAllCachedRolesWithoutReloading() {
        entries.put("rbac:role:permissions:t001:A", Set.of("a:read"));
        entries.put("rbac:role:permissions:t001:B", Set.of("b:write"));

        assertThat(service.findRolePermission(List.of("A", "B"), "7", "t001"))
                .containsExactlyInAnyOrder("a:read", "b:write");
        verify(loader, never()).loadPermissions(any(), any());
    }

    @Test
    void shouldLoadAndCacheAllRolesWhenNothingIsCached() {
        when(loader.loadPermissions(any(), eq("t001")))
                .thenReturn(Map.of("A", Set.of("a:read"), "B", Set.of("b:write")));

        assertThat(service.findRolePermission(List.of("A", "B"), "7", "t001"))
                .containsExactlyInAnyOrder("a:read", "b:write");
        assertThat(entries).containsEntry("rbac:role:permissions:t001:A", Set.of("a:read"))
                .containsEntry("rbac:role:permissions:t001:B", Set.of("b:write"));
    }

    @Test
    void shouldNotGrantPermissionsForRolesTheUserDoesNotOwn() {
        entries.put("rbac:role:permissions:t001:A", Set.of("a:read"));
        entries.put("rbac:role:permissions:t001:ADMIN", Set.of("all:write"));

        assertThat(service.findRolePermission(List.of("A", "ADMIN"), "7", "t001"))
                .containsExactly("a:read");
        verify(loader, never()).loadPermissions(any(), any());
    }

    @Test
    void shouldReturnEmptyPermissionsWithoutAnyActiveRole() {
        assertThat(service.findRolePermission(List.of(), "7", "t001")).isEmpty();
        assertThat(service.findRolePermission(List.of("UNKNOWN"), "7", "t001")).isEmpty();
        verify(loader, never()).loadPermissions(any(), any());
    }
}
