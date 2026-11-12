package com.github.zeng.alt.security.core.reactive.handler;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证默认错误处理器原有无参工厂仍可直接使用。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class DefaultReactiveHandlersTest {
    @Test
    void shouldCreateUnauthorizedResponseFromLegacyEntryPointFactory() {
        final var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/secured").build());

        StepVerifier.create(DefaultReactiveAuthenticationEntryPoint.handler()
                .commence(exchange, new BadCredentialsException("unauthenticated"))).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getBodyAsString().block()).contains("\"status\":401");
    }

    @Test
    void shouldCreateForbiddenResponseFromLegacyDeniedHandlerFactory() {
        final var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/secured").build());

        StepVerifier.create(DefaultReactiveAccessDeniedHandler.handler()
                .handle(exchange, new AccessDeniedException("denied"))).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exchange.getResponse().getBodyAsString().block()).contains("\"status\":403");
    }
}
