package com.github.zeng.alt.security.rbac.serve.locator;

import com.github.zeng.alt.security.api.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.List;

/**
 * Servlet 环境资源定位器接口。
 *
 * <p>根据认证信息加载用户可访问的 HTTP 资源列表，并支持查询资源所需的权限标识。</p>
 */
public interface ResourceLocator {

    List<Resource> load(Authentication authentication) throws AuthenticationException;

    boolean supports(Class<?> resource);

    /**
     * 查询指定资源所需的权限标识。
     *
     * @param resource       资源
     * @param authentication 认证信息
     * @return 权限标识，无匹配时返回 {@code null}
     */
    default String loadPermissionForResource(Resource resource, Authentication authentication) {
        return null;
    }
}
