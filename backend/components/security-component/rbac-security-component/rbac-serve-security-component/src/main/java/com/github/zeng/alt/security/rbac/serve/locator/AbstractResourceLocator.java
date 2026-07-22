package com.github.zeng.alt.security.rbac.serve.locator;

import com.github.zeng.alt.security.api.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.ArrayList;
import java.util.List;

/**
 * Servlet 环境资源定位器的抽象基类。
 *
 * <p>提供匿名用户过滤和认证信息提取的通用逻辑，
 * 子类需实现以下方法：</p>
 * <ul>
 *   <li>{@link #list(Object)} — 根据认证主体加载资源列表</li>
 *   <li>{@link #loadPermissionForResource(Resource, Object)} — 根据资源获取所需的权限标识</li>
 *   <li>{@link #verifyInstance(Resource)} — 校验资源类型</li>
 * </ul>
 */
@Slf4j
public abstract class AbstractResourceLocator implements ResourceLocator {

    protected abstract List<Resource> list(@Nullable Object principal);

    /**
     * 根据资源获取所需的权限标识。
     *
     * @param resource 资源
     * @param principal 认证主体
     * @return 权限标识，无匹配时返回空字符串
     */
    protected abstract String loadPermissionForResource(Resource resource, @Nullable Object principal);

    /**
     * 校验资源类型是否受此定位器支持。
     *
     * @param resource 资源
     */
    protected abstract void verifyInstance(Resource resource);

    private Object getAuthorizationPrincipal(Authentication authentication) {
        return authentication.getPrincipal();
    }

    private boolean isNotAnonymous(Authentication authentication) {
        return authentication.isAuthenticated();
    }

    @Override
    public List<Resource> load(Authentication authentication) throws AuthenticationException {
        if (!isNotAnonymous(authentication)) {
            return new ArrayList<>();
        }
        return list(getAuthorizationPrincipal(authentication));
    }

    @Override
    public String loadPermissionForResource(Resource resource, Authentication authentication) {
        if (!isNotAnonymous(authentication)) {
            return "";
        }
        return loadPermissionForResource(resource, getAuthorizationPrincipal(authentication));
    }
}
