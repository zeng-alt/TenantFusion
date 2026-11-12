package com.github.zeng.alt.security.core.reactive.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.zeng.alt.security.api.ReactiveAuthorizationManagerProvider;
import com.github.zeng.alt.security.api.WhiteListService;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证完整 WebFlux 安全链在拒绝请求时返回 ProblemDetail，不到达业务入口。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class ReactiveSecurityErrorResponseTest {
    @Test
    void shouldReturnUnauthorizedProblemForAnonymousRequest() {
        assertProblemResponse(null, HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturnForbiddenProblemForAuthenticatedRequest() {
        assertProblemResponse(new TestingAuthenticationToken("review", "unused", "operator"), HttpStatus.FORBIDDEN);
    }

    private void assertProblemResponse(Authentication authentication, HttpStatus expected) {
        new ReactiveWebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ReactiveSecurityAutoConfiguration.class))
                .withUserConfiguration(DenyAllProvider.class)
                .withBean(ObjectMapper.class, () -> new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT))
                .withBean(SecurityProperties.class, SecurityProperties::new)
                .withBean(WhiteListService.class, () -> Set::of)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    final var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/secured").build());
                    final var reached = new AtomicInteger();
                    final Mono<Void> response = new WebFilterChainProxy(context.getBean(SecurityWebFilterChain.class))
                            .filter(exchange, ignored -> {
                                reached.incrementAndGet();
                                return Mono.empty();
                            });
                    StepVerifier.create(authentication == null ? response : response.contextWrite(
                            ReactiveSecurityContextHolder.withAuthentication(authentication))).verifyComplete();
                    assertThat(reached).hasValue(0);
                    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(expected);
                    assertThat(exchange.getResponse().getHeaders().getContentType())
                            .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
                    final String body = exchange.getResponse().getBodyAsString().block();
                    assertThat(body).contains("\n");
                    final var problem = new ObjectMapper().readTree(body);
                    assertThat(problem.get("status").asInt()).isEqualTo(expected.value());
                    assertThat(problem.get("instance").asText()).isEqualTo("/secured");
                    assertThat(problem.get("title").asText()).isNotBlank();
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class DenyAllProvider {
        @Bean
        ReactiveAuthorizationManagerProvider<AuthorizationContext> denyAll() {
            return () -> (authentication, context) -> Mono.just(new AuthorizationDecision(false));
        }
    }
}
