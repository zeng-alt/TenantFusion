package com.github.zeng.alt.security.rbac.serve.repository;

import com.github.zeng.alt.security.api.Resource;
import com.github.zeng.alt.storage.StorageTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DefaultRbacResourceService implements RbacResourceService {

    private static final String RESOURCES_KEY_PREFIX = "rbac:resources:";
    private static final String PERMISSION_KEY_PREFIX = "rbac:permission:";
    private static final String ROLE_PERMISSIONS_KEY_PREFIX = "rbac:role:permissions:";

    private final StorageTemplate storageTemplate;

    public DefaultRbacResourceService(StorageTemplate storageTemplate) {
        this.storageTemplate = storageTemplate;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Resource> findAllHttpResource(String username, String tenantName, List<String> authorities) {
        String key = RESOURCES_KEY_PREFIX + tenantName + ":" + username;
        List<Resource> cached = storageTemplate.opsForString().get(key, List.class);
        return cached != null ? cached : Collections.emptyList();
    }

    @Override
    public String findPermissionByResource(String tenantName, String resourceKey) {
        String key = PERMISSION_KEY_PREFIX + tenantName + ":" + resourceKey;
        return storageTemplate.opsForString().get(key, String.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<String> findPermission(List<String> authorities, String tenantName) {
        if (authorities == null || authorities.isEmpty()) {
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
        return result;
    }

    public void setResources(String tenantName, String username, List<Resource> resources) {
        storageTemplate.opsForString().set(RESOURCES_KEY_PREFIX + tenantName + ":" + username, resources);
    }

    public void setPermissionForResource(String tenantName, String resourceKey, String permission) {
        storageTemplate.opsForString().set(PERMISSION_KEY_PREFIX + tenantName + ":" + resourceKey, permission);
    }

    public void setPermissionsForRole(String tenantName, String roleAuthority, Set<String> permissions) {
        storageTemplate.opsForString().set(ROLE_PERMISSIONS_KEY_PREFIX + tenantName + ":" + roleAuthority, permissions);
    }
}
