package com.github.zeng.alt.security.rbac.serve.locator;


import com.github.zeng.alt.security.api.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Reactive 环境资源定位器的抽象基类。
 *
 * <p>提供匿名用户过滤和认证信息提取的通用逻辑，
 * 子类需实现以下抽象方法：</p>
 * <ul>
 *   <li>{@link #list(Object)} — 根据认证主体加载资源列表</li>
 *   <li>{@link #loadPermissionForResource(Resource, Object)} — 根据资源获取所需的权限标识</li>
 *   <li>{@link #verifyInstance(Resource)} — 校验资源类型</li>
 * </ul>
 */
@Slf4j
public abstract class AbstractReactiveResourceLocator implements ReactiveResourceLocator {

    protected abstract List<Resource> list(@Nullable Object principal);
    protected abstract String loadPermissionForResource(Resource resource, @Nullable Object principal);

    protected abstract void verifyInstance(Resource resource);

    private Object getAuthorizationPrincipal(Authentication authentication) {
        return authentication.getPrincipal();
    }

    private boolean isNotAnonymous(Authentication authentication) {
        return authentication.isAuthenticated();
    }

    @Override
    public Mono<List<Resource>> load(Mono<Authentication> authentication) {
        return authentication
                .filter(this::isNotAnonymous)
                .map(this::getAuthorizationPrincipal)
                .map(this::list)
                .switchIfEmpty(Mono.empty());
    }

    @Override
    public Mono<String> load(Resource resource, Mono<Authentication> authentication) {
        return authentication
                .filter(this::isNotAnonymous)
                .map(this::getAuthorizationPrincipal)
                .map(principal -> loadPermissionForResource(resource, principal))
                .switchIfEmpty(Mono.empty());
    }

    @Override
    public Mono<Set<String>> load(Set<Resource> resources, Mono<Authentication> authentication) {
        return authentication
                .filter(this::isNotAnonymous)
                .map(principal -> Collections.<String>emptySet());
    }
}
