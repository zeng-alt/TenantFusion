package com.github.zeng.alt.security.rbac.serve.manager;

import com.github.zeng.alt.security.api.Resource;
import com.github.zeng.alt.security.rbac.serve.locator.ResourceLocator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Servlet 环境资源查询管理器。
 *
 * <p>根据资源类型查找匹配的 {@link ResourceLocator}，支持：</p>
 * <ul>
 *   <li>加载用户资源列表 — {@link #query(Resource, Authentication)}</li>
 *   <li>查询资源所需权限 — {@link #queryPermissionForResource(Resource, Authentication)}</li>
 *   <li>批量查询资源权限 — {@link #queryPermissionsForResources(Set, Authentication)}</li>
 * </ul>
 */
@Slf4j
public class ResourceQueryManager {

    private final List<ResourceLocator> resourceLocators;

    public ResourceQueryManager(List<ResourceLocator> resourceLocators) {
        this.resourceLocators = resourceLocators;
        log.debug("ResourceQueryManager initialized with {} locators", resourceLocators.size());
    }

    /**
     * 查询当前用户可访问的指定类型资源。
     *
     * @param resource       资源类型实例（用于匹配 locator）
     * @param authentication 当前认证信息
     * @return 用户可访问的资源列表
     */
    public List<Resource> query(Resource resource, Authentication authentication) {
        return resourceLocators
                .stream()
                .filter(locator -> locator.supports(resource.getClass()))
                .findFirst()
                .map(locator -> locator.load(authentication))
                .orElse(new ArrayList<>());
    }

    /**
     * 查询指定资源所需的权限标识。
     *
     * @param resource       资源
     * @param authentication 当前认证信息
     * @return 权限标识，无匹配时返回 {@code null}
     */
    public String queryPermissionForResource(Resource resource, Authentication authentication) {
        return resourceLocators
                .stream()
                .filter(locator -> locator.supports(resource.getClass()))
                .findFirst()
                .map(locator -> locator.loadPermissionForResource(resource, authentication))
                .orElse(null);
    }

    /**
     * 批量查询资源所需的权限标识集合。
     *
     * @param resources      资源集合
     * @param authentication 当前认证信息
     * @return 权限标识集合，无匹配时返回空集合
     */
    public Set<String> queryPermissionsForResources(Set<Resource> resources, Authentication authentication) {
        if (CollectionUtils.isEmpty(resources)) {
            return Set.of();
        }
        return resourceLocators
                .stream()
                .filter(locator -> locator.supports(resources.iterator().next().getClass()))
                .findFirst()
                .map(locator -> {
                    log.trace("Querying permissions for {} resources", resources.size());
                    return locator.loadPermissionForResource(resources.iterator().next(), authentication);
                })
                .map(Set::of)
                .orElse(Set.of());
    }
}
