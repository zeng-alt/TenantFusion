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
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年12月09日 21:47
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
