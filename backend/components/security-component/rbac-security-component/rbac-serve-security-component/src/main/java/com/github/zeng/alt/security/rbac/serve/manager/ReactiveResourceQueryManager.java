package com.github.zeng.alt.security.rbac.serve.manager;

import com.github.zeng.alt.security.api.Resource;
import com.github.zeng.alt.security.rbac.serve.locator.ReactiveResourceLocator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

/**
 * Reactive 环境资源查询管理器。
 *
 * <p>根据资源类型查找匹配的 {@link ReactiveResourceLocator}，支持：</p>
 * <ul>
 *   <li>加载用户资源列表 — {@link #query(Resource, Mono)}</li>
 *   <li>查询资源所需权限 — {@link #queryPermissionForResource(Resource, Mono)}</li>
 *   <li>批量查询资源权限 — {@link #queryPermissionsForResources(Set, Mono)}</li>
 * </ul>
 */
@Slf4j
public class ReactiveResourceQueryManager {

    private final List<ReactiveResourceLocator> resourceLocators;

    public ReactiveResourceQueryManager(List<ReactiveResourceLocator> resourceLocators) {
        this.resourceLocators = resourceLocators;
        log.debug("ReactiveResourceQueryManager initialized with {} locators", resourceLocators.size());
    }

    public Mono<List<Resource>> query(Resource resource, Mono<Authentication> authentication) {
        return Flux
                .fromIterable(this.resourceLocators)
                .filter(locator -> locator.supports(resource.getClass()))
                .next()
                .flatMap(locator -> locator.load(authentication))
                .switchIfEmpty(Mono.empty());
    }

    public Mono<String> queryPermissionForResource(Resource resource, Mono<Authentication> authentication) {
        return Flux
                .fromIterable(this.resourceLocators)
                .filter(locator -> locator.supports(resource.getClass()))
                .next()
                .flatMap(locator -> locator.load(resource, authentication))
                .switchIfEmpty(Mono.empty());
    }

    public Mono<Set<String>> queryPermissionsForResources(Set<Resource> resource, Mono<Authentication> authentication) {
        if (CollectionUtils.isEmpty(resource)) {
            return Mono.empty();
        }

        return Flux
                .fromIterable(this.resourceLocators)
                .filter(locator -> locator.supports(resource.iterator().next().getClass()))
                .next()
                .flatMap(locator -> locator.load(resource, authentication));
    }
}
