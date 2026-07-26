package com.github.zeng.alt.security.rbac.serve.locator;

import com.github.zeng.alt.security.api.Resource;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

/**
 * 根据资源获取资源标识
 * @author zengJiaJun
 * @since 2026年07月25日
 * @version 1.0
 */
public interface ReactiveResourceSignageLocator {

    Mono<String> load(Resource resource, Authentication authentication);

}

