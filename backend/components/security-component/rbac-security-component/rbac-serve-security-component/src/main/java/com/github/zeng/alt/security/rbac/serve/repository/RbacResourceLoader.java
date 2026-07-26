package com.github.zeng.alt.security.rbac.serve.repository;

import com.github.zeng.alt.security.api.Resource;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RbacResourceLoader {

    String loadPermissionByResource(String tenantName, Resource resource);

    Map<String, Set<String>> loadPermissions(List<String> authorities, String tenantName);

    Set<String> loadUserRole(String userId, String tenantName);
}
