package com.github.zeng.alt.security.rbac.serve.manager;

import com.github.zeng.alt.security.api.Resource;
import com.github.zeng.alt.security.rbac.serve.locator.ReactiveResourceLocator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年12月05日 21:14
 */
@RequiredArgsConstructor
public class ReactiveResourceQueryManager {

    private final List<ReactiveResourceLocator> resourceLocators;

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
                .filter(locator -> {
                    return locator.supports(resource.iterator().next().getClass());
                })
                .next()
                .flatMap(locator -> {
                    return locator.load(resource, authentication);
                });
    }
}
