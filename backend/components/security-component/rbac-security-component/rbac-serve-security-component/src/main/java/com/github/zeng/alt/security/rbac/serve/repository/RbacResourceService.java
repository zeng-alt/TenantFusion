package com.github.zeng.alt.security.rbac.serve.repository;

import com.github.zeng.alt.security.api.Resource;
import com.github.zeng.alt.security.api.UserContextHolder;

import java.util.List;
import java.util.Set;

public interface RbacResourceService {

    Set<String> findRolePermission(List<String> authorities, String userId, String tenantName);

    String findPermission(String tenantName, Resource resource);

    Set<String> findUserRole(String userId, String tenantName);

    void removePermission(List<Resource> resources, String tenantName);

    void removeUserRole(List<String> userIds, String tenantName);

    void removeRolePermission(List<String> roleCodes, String tenantName);

    default void removePermission(List<Resource> resources) {
        removePermission(resources, UserContextHolder.getTenant());
    }

    default void removeUserRole(List<String> userIds) {
        removeUserRole(userIds, UserContextHolder.getTenant());
    }

    default void removeRolePermission(List<String> roleCodes) {
        removeRolePermission(roleCodes, UserContextHolder.getTenant());
    }
}
