package com.github.zeng.alt.security.core.reactive.config;

import com.github.zeng.alt.security.api.ReactiveAuthorizationConstraint;
import com.github.zeng.alt.security.api.ReactiveAuthorizationManagerProvider;
import com.github.zeng.alt.security.api.WhiteListService;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import com.github.zeng.alt.security.core.reactive.AuthorizeExchangeCustomizer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.security.web.server.authorization.AuthorizationWebFilter;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import org.springframework.web.server.handler.DefaultWebFilterChain;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 WebFlux 自动配置分别装配 OR 授权与有序 AND 约束。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class ReactiveSecurityAutoConfigurationTest {

    private final ReactiveWebApplicationContextRunner runner = new ReactiveWebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ReactiveSecurityAutoConfiguration.class))
            .withBean(SecurityProperties.class, SecurityProperties::new)
            .withBean(WhiteListService.class, () -> () -> Set.of("/public"));

    @Test
    void withoutConstraintsKeepsWhitelistAndAuthenticatedFallback() {
        runner.run(context -> {
            StepVerifier.create(authorize(context, "/public", null)).verifyComplete();
            StepVerifier.create(authorize(context, "/secured", null))
                    .expectError(AccessDeniedException.class).verify();
            StepVerifier.create(authorize(
                    context, "/secured", new TestingAuthenticationToken("user", "unused", "ROLE_USER")
            )).verifyComplete();
        });
    }

    @Test
    void constraintsVetoWhitelistInOrderAndShortCircuit() {
        runner.withUserConfiguration(OrderedConstraints.class).run(context -> {
            StepVerifier.create(authorize(context, "/public", null))
                    .expectError(AccessDeniedException.class).verify();
            assertThat(context.getBean(OrderedConstraints.class).calls).containsExactly("first", "deny");
        });
    }

    @Test
    void constraintsVetoAuthenticatedFallback() {
        runner.withUserConfiguration(OrderedConstraints.class).run(context -> {
            StepVerifier.create(authorize(
                    context, "/secured", new TestingAuthenticationToken("user", "unused", "ROLE_USER")
            )).expectError(AccessDeniedException.class).verify();
            assertThat(context.getBean(OrderedConstraints.class).calls).containsExactly("first", "deny");
        });
    }

    @Test
    void constraintsCannotGrantWhenBaseDenies() {
        runner.withUserConfiguration(OrderedConstraints.class).run(context -> {
            StepVerifier.create(authorize(context, "/secured", null))
                    .expectError(AccessDeniedException.class).verify();
            assertThat(context.getBean(OrderedConstraints.class).calls).isEmpty();
        });
    }

    @Test
    void providersRemainOrderedOrAlternatives() {
        runner.withUserConfiguration(OrderedProviders.class).run(context -> {
            StepVerifier.create(authorize(context, "/secured", null)).verifyComplete();
            assertThat(context.getBean(OrderedProviders.class).calls).containsExactly("deny", "allow");
        });
    }

    @Test
    void providerGrantStillRequiresConstraints() {
        runner.withUserConfiguration(OrderedProviders.class, OrderedConstraints.class).run(context -> {
            StepVerifier.create(authorize(context, "/secured", null))
                    .expectError(AccessDeniedException.class).verify();
            assertThat(context.getBean(OrderedProviders.class).calls).containsExactly("deny", "allow");
            assertThat(context.getBean(OrderedConstraints.class).calls).containsExactly("first", "deny");
        });
    }

    @Test
    void constraintsVetoCustomAuthenticatedAndPublicPaths() {
        runner.withUserConfiguration(OrderedConstraints.class)
                .withBean(AuthorizeExchangeCustomizer.class, () -> registry -> registry
                        .pathMatchers("/custom/private").authenticated()
                        .pathMatchers("/custom/public").permitAll())
                .run(context -> {
                    final var auth = new TestingAuthenticationToken("user", "unused", "ROLE_USER");
                    StepVerifier.create(authorize(context, "/custom/private", auth))
                            .expectError(AccessDeniedException.class).verify();
                    StepVerifier.create(authorize(context, "/custom/public", null))
                            .expectError(AccessDeniedException.class).verify();
                });
    }

    @Test
    void shouldNotPermitUnconfiguredLoginPaths() {
        runner.run(context -> {
            final var exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.method(HttpMethod.POST, "/login/unrelated").build());
            StepVerifier.create(context.getBean(SecurityWebFilterChain.class).getWebFilters()
                    .filter(AuthorizationWebFilter.class::isInstance).cast(AuthorizationWebFilter.class).single()
                    .flatMap(filter -> filter.filter(exchange, ignored -> Mono.empty())))
                    .expectError(AccessDeniedException.class).verify();
        });
    }

    private Mono<Void> authorize(ApplicationContext context, String path, Authentication authentication) {
        final var exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path).build());
        final Mono<Void> authorization = context.getBean(SecurityWebFilterChain.class).getWebFilters()
                .filter(AuthorizationWebFilter.class::isInstance)
                .collectList()
                .flatMap(filters -> new DefaultWebFilterChain(ignored -> Mono.empty(), filters).filter(exchange));
        return authentication == null ? authorization
                : authorization.contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
    }

    @Configuration(proxyBeanMethods = false)
    static class OrderedConstraints {
        private final List<String> calls = new ArrayList<>();

        @Bean
        @Order(20)
        ReactiveAuthorizationConstraint<AuthorizationContext> lastConstraint() {
            return (authentication, context) -> {
                calls.add("unreached");
                return Mono.just(new AuthorizationDecision(true));
            };
        }

        @Bean
        @Order(10)
        ReactiveAuthorizationConstraint<AuthorizationContext> denyingConstraint() {
            return (authentication, context) -> {
                calls.add("deny");
                return Mono.just(new AuthorizationDecision(false));
            };
        }

        @Bean
        @Order(-10)
        ReactiveAuthorizationConstraint<AuthorizationContext> firstConstraint() {
            return (authentication, context) -> {
                calls.add("first");
                return Mono.just(new AuthorizationDecision(true));
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class OrderedProviders {
        private final List<String> calls = new ArrayList<>();

        @Bean
        @Order(10)
        ReactiveAuthorizationManagerProvider<AuthorizationContext> allowingProvider() {
            return () -> (authentication, context) -> {
                calls.add("allow");
                return Mono.just(new AuthorizationDecision(true));
            };
        }

        @Bean
        @Order(-10)
        ReactiveAuthorizationManagerProvider<AuthorizationContext> denyingProvider() {
            return () -> (authentication, context) -> {
                calls.add("deny");
                return Mono.just(new AuthorizationDecision(false));
            };
        }
    }
}
