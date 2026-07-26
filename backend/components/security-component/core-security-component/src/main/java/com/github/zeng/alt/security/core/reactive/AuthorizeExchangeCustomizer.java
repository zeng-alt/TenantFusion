package com.github.zeng.alt.security.core.reactive;

import org.springframework.security.config.web.server.ServerHttpSecurity;

/**
 * @author zengJiaJun
 * @since 2026年07月25日
 * @version 1.0
 */
@FunctionalInterface
public interface AuthorizeExchangeCustomizer {

    void customize(ServerHttpSecurity.AuthorizeExchangeSpec registry);

}

