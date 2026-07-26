package com.github.zeng.alt.security.rbac.serve.repository;

import com.github.zeng.alt.security.api.Resource;
import com.github.zeng.alt.storage.StorageTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
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
 *   <li>用户角色列表：{@code rbac:user:role:{tenant}:{userId}} → {@code Set<String>}</li>
 *   <li>资源权限映射：{@code rbac:permission:{tenant}:{resourceKey}} → {@code String}</li>
 *   <li>角色权限集合：{@code rbac:role:permissions:{tenant}:{authority}} → {@code Set<String>}</li>
 * </ul>
 */
@Slf4j
public class DefaultRbacResourceService implements RbacResourceService {

    public static final String RESOURCES_KEY_PREFIX = "rbac:resources:";
    public static final String PERMISSION_KEY_PREFIX = "rbac:permission:";
    public static final String ROLE_PERMISSIONS_KEY_PREFIX = "rbac:role:permissions:";
    public static final String USER_ROLE_KEY_PREFIX = "rbac:user:role:";

    /** rbac:user:role:* — 用户角色列表 */
    public static final Duration USER_ROLE_TIME = Duration.ofMinutes(10);
    /** rbac:role:permissions:* — 角色权限集合 */
    public static final Duration ROLE_PERMISSIONS_TIME = Duration.ofMinutes(15);
    /** rbac:permission:* — 资源→权限映射 */
    public static final Duration PERMISSIONS_TIME = Duration.ofMinutes(30);
    /** rbac:resources:* — 用户资源列表 */
    public static final Duration RESOURCES_TIME = Duration.ofMinutes(10);


    private final StorageTemplate storageTemplate;
    private final RbacResourceLoader resourceLoader;

    public DefaultRbacResourceService(StorageTemplate storageTemplate, RbacResourceLoader resourceLoader) {
        this.storageTemplate = storageTemplate;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public String findPermission(String tenantName, Resource resource) {
        String key = PERMISSION_KEY_PREFIX + tenantName + ":" + resource.getKey();
        log.trace("Fetching permission for key: {}", key);
        String cached = storageTemplate.opsForString().get(key, String.class);
        if (cached != null) {
            log.trace("Cache hit for permission key: {} -> {}", key, cached);
            return cached;
        }
        log.debug("Cache miss for permission key: {}, loading from RbacResourceLoader", key);
        String loaded = resourceLoader.loadPermissionByResource(tenantName, resource);
        if (loaded != null) {
            log.debug("Caching permission for key: {} -> {}", key, loaded);
            storageTemplate.opsForString().set(key, loaded, PERMISSIONS_TIME);
        }
        return loaded;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<String> findRolePermission(List<String> authorities, String userId, String tenantName) {
        if (CollectionUtils.isEmpty(authorities)) {
            log.trace("No authorities provided, returning empty permissions");
            return Collections.emptySet();
        }

        Set<String> userRole = findUserRole(userId, tenantName);
        authorities = userRole.stream()
                .filter(authorities::contains)
                .toList();

        if (CollectionUtils.isEmpty(authorities)) {
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
                storageTemplate.opsForString().set(authKey, entry.getValue(), ROLE_PERMISSIONS_TIME);
                result.addAll(entry.getValue());
            }
        }
        return result;
    }


    @Override
    public Set<String> findUserRole(String id, String tenantName) {
        if (!StringUtils.hasText(id)) {
            return Set.of();
        }
        String key = USER_ROLE_KEY_PREFIX + tenantName + ":" + id;
        Set<String> roleCodes = storageTemplate.opsForString().get(key, Set.class);
        if (roleCodes != null) {
            return roleCodes;
        }
        log.info("Cache miss for userRole (tenant '{}', {} authorities), loading from RbacResourceLoader",
                tenantName, id);

        Set<String> loaded = resourceLoader.loadUserRole(id, tenantName);
        storageTemplate.opsForString().set(key, loaded, USER_ROLE_TIME);
        return loaded;
    }

    @Override
    public void removePermission(List<Resource> resources, String tenantName) {
        String[] keys = resources
                .stream()
                .map(resource -> PERMISSION_KEY_PREFIX + tenantName + ":" + resource.getKey())
                .toArray(String[]::new);
        storageTemplate.opsForString().delete(keys);
    }

    @Override
    public void removeUserRole(List<String> userIds, String tenantName) {
        String[] keys = userIds
                .stream()
                .map(id -> USER_ROLE_KEY_PREFIX + tenantName + ":" + id)
                .toArray(String[]::new);
        storageTemplate.opsForString().delete(keys);
    }

    @Override
    public void removeRolePermission(List<String> roleCodes, String tenantName) {
        String[] keys = roleCodes
                .stream()
                .map(authority -> ROLE_PERMISSIONS_KEY_PREFIX + tenantName + ":" + authority)
                .toArray(String[]::new);
        storageTemplate.opsForString().delete(keys);
    }
}
