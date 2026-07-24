package com.github.zeng.alt.security.rbac.serve.repository;

import com.github.zeng.alt.security.api.Resource;
import com.github.zeng.alt.storage.StorageTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 基于 {@link StorageTemplate} 的默认 RBAC 权限数据服务实现。
 *
 * <p>缓存穿透 + 回填模式：优先查询缓存，缓存未命中时委托 {@link RbacResourceLoader} 加载并自动回填缓存。</p>
 *
 * <p>存储结构（KV 缓存）：</p>
 * <ul>
 *   <li>用户资源列表：{@code rbac:resources:{tenant}:{username}} → {@code List<Resource>}</li>
 *   <li>资源权限映射：{@code rbac:permission:{tenant}:{resourceKey}} → {@code String}</li>
 *   <li>角色权限集合：{@code rbac:role:permissions:{tenant}:{authority}} → {@code Set<String>}</li>
 * </ul>
 */
@Slf4j
public class DefaultRbacResourceService implements RbacResourceService {

    private static final String RESOURCES_KEY_PREFIX = "rbac:resources:";
    private static final String PERMISSION_KEY_PREFIX = "rbac:permission:";
    private static final String ROLE_PERMISSIONS_KEY_PREFIX = "rbac:role:permissions:";

    private final StorageTemplate storageTemplate;
    private final RbacResourceLoader resourceLoader;

    public DefaultRbacResourceService(StorageTemplate storageTemplate, RbacResourceLoader resourceLoader) {
        this.storageTemplate = storageTemplate;
        this.resourceLoader = resourceLoader;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Resource> findAllHttpResource(String username, String tenantName, List<String> authorities) {
        String key = RESOURCES_KEY_PREFIX + tenantName + ":" + username;
        log.debug("Fetching resources for key: {}", key);
        List<Resource> cached = storageTemplate.opsForString().get(key, List.class);
        if (cached != null) {
            log.debug("Cache hit for resources key: {} ({} items)", key, cached.size());
            return cached;
        }
        log.info("Cache miss for resources key: {}, loading from RbacResourceLoader", key);
        List<Resource> loaded = resourceLoader.loadHttpResources(username, tenantName, authorities);
        if (!CollectionUtils.isEmpty(loaded)) {
            log.debug("Caching {} resources for key: {}", loaded.size(), key);
            storageTemplate.opsForString().set(key, loaded);
        }
        return loaded != null ? loaded : Collections.emptyList();
    }

    @Override
    public String findPermissionByResource(String tenantName, String resourceKey) {
        if (!StringUtils.hasText(resourceKey)) {
            return null;
        }
        int idx = resourceKey.lastIndexOf(':');
        if (idx <= 0 || idx == resourceKey.length() - 1) {
            log.warn("Invalid resource key format: {}", resourceKey);
            return null;
        }
        return findPermissionByMethodAndPath(tenantName, resourceKey.substring(idx + 1), resourceKey.substring(0, idx));
    }

    @Override
    public String findPermissionByMethodAndPath(String tenantName, String method, String path) {
        String key = PERMISSION_KEY_PREFIX + tenantName + ":" + method + ":" + path;
        log.trace("Fetching permission for key: {}", key);
        String cached = storageTemplate.opsForString().get(key, String.class);
        if (cached != null) {
            log.trace("Cache hit for permission key: {} -> {}", key, cached);
            return cached;
        }
        log.debug("Cache miss for permission key: {}, loading from RbacResourceLoader", key);
        String loaded = resourceLoader.loadPermissionByMethodAndPath(tenantName, method, path);
        if (loaded != null) {
            log.debug("Caching permission for key: {} -> {}", key, loaded);
            storageTemplate.opsForString().set(key, loaded);
        }
        return loaded;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<String> findPermission(List<String> authorities, String tenantName) {
        if (CollectionUtils.isEmpty(authorities)) {
            log.trace("No authorities provided, returning empty permissions");
            return Collections.emptySet();
        }
        Set<String> result = new HashSet<>();
        for (String authority : authorities) {
            String key = ROLE_PERMISSIONS_KEY_PREFIX + tenantName + ":" + authority;
            Set<String> perms = storageTemplate.opsForString().get(key, Set.class);
            if (perms != null) {
                result.addAll(perms);
            }
        }
        if (!result.isEmpty()) {
            log.debug("All {} authorities cache-hit in tenant '{}', total {} permissions",
                    authorities.size(), tenantName, result.size());
            return result;
        }
        log.info("Cache miss for permissions (tenant '{}', {} authorities), loading from RbacResourceLoader",
                tenantName, authorities.size());
        Map<String, Set<String>> loaded = resourceLoader.loadPermissions(authorities, tenantName);
        if (loaded != null) {
            for (Map.Entry<String, Set<String>> entry : loaded.entrySet()) {
                String authKey = ROLE_PERMISSIONS_KEY_PREFIX + tenantName + ":" + entry.getKey();
                log.debug("Caching {} permissions for authority '{}'", entry.getValue().size(), entry.getKey());
                storageTemplate.opsForString().set(authKey, entry.getValue());
                result.addAll(entry.getValue());
            }
        }
        return result;
    }

    public void setResources(String tenantName, String username, List<Resource> resources) {
        String key = RESOURCES_KEY_PREFIX + tenantName + ":" + username;
        log.debug("Setting resources for key: {} ({} items)", key, resources.size());
        storageTemplate.opsForString().set(key, resources);
    }

    public void setPermissionForResource(String tenantName, String resourceKey, String permission) {
        String key = PERMISSION_KEY_PREFIX + tenantName + ":" + resourceKey;
        log.debug("Setting permission for key: {} -> {}", key, permission);
        storageTemplate.opsForString().set(key, permission);
    }

    public void setPermissionsForRole(String tenantName, String roleAuthority, Set<String> permissions) {
        String key = ROLE_PERMISSIONS_KEY_PREFIX + tenantName + ":" + roleAuthority;
        log.debug("Setting role permissions for key: {} ({} items)", key, permissions.size());
        storageTemplate.opsForString().set(key, permissions);
    }
}
