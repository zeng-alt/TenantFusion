package com.github.zeng.alt.security.rbac.serve.repository;

import com.github.zeng.alt.security.api.Resource;

import java.util.List;
import java.util.Set;

public interface RbacResourceService {

    List<Resource> findAllHttpResource(String username, String tenantName, List<String> authorities);

    String findPermissionByResource(String tenantName, String resourceKey);

    String findPermissionByMethodAndPath(String tenantName, String method, String path);

    Set<String> findPermission(List<String> authorities, String tenantName);
}
