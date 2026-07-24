package com.github.zeng.alt.security.rbac.serve.repository;

import com.github.zeng.alt.security.api.Resource;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RbacResourceLoader {

    List<Resource> loadHttpResources(String username, String tenantName, List<String> authorities);

    String loadPermissionByResource(String tenantName, String resourceKey);

    String loadPermissionByMethodAndPath(String tenantName, String method, String path);

    Map<String, Set<String>> loadPermissions(List<String> authorities, String tenantName);
}
