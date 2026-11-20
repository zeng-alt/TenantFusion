package com.github.zeng.alt.security.core.reactive.config;

import com.github.zeng.alt.security.api.ReactiveAuthorizationManagerProvider;
import com.github.zeng.alt.security.api.WhiteListRule;
import com.github.zeng.alt.security.core.properties.HttpWhiteListProperties;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.security.web.server.authorization.AuthorizationWebFilter;
import org.springframework.web.server.handler.DefaultWebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 WebFlux 两类白名单与 Servlet 保持终结授权语义一致。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class ReactiveWhiteListAuthorizationTest {
    private final ReactiveWebApplicationContextRunner runner = new ReactiveWebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ReactiveSecurityAutoConfiguration.class))
            .withBean(SecurityProperties.class, SecurityProperties::new)
            .withPropertyValues("security.whitelist.permit-all[0].method=GET",
                    "security.whitelist.permit-all[0].path=/public",
                    "security.whitelist.authenticated[0].method=PATCH",
                    "security.whitelist.authenticated[0].path=/profile/{id}");

    @Test
    void publicRuleDoesNotRequireAuthenticationOrCallProviders() {
        final var calls = new AtomicInteger();
        runner.withBean(ReactiveAuthorizationManagerProvider.class, () -> provider(calls, false)).run(context -> {
            StepVerifier.create(authorize(context, HttpMethod.GET, "/public", null)).verifyComplete();
            assertThat(calls).hasValue(0);
        });
    }

    @Test
    void loginRuleCannotFallThroughToGrantingProvider() {
        final var calls = new AtomicInteger();
        runner.withBean(ReactiveAuthorizationManagerProvider.class, () -> provider(calls, true)).run(context -> {
            StepVerifier.create(authorize(context, HttpMethod.PATCH, "/profile/7", null))
                    .expectError(AccessDeniedException.class).verify();
            final var anonymous = new AnonymousAuthenticationToken("key", "anonymous",
                    AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
            StepVerifier.create(authorize(context, HttpMethod.PATCH, "/profile/7", anonymous))
                    .expectError(AccessDeniedException.class).verify();
            assertThat(calls).hasValue(0);
        });
    }

    @Test
    void loginRuleSkipsProvidersButOtherMethodsRemainProtected() {
        final var calls = new AtomicInteger();
        runner.withBean(ReactiveAuthorizationManagerProvider.class, () -> provider(calls, false)).run(context -> {
            final var user = new TestingAuthenticationToken("user", "unused", "ROLE_USER");
            StepVerifier.create(authorize(context, HttpMethod.PATCH, "/profile/7", user)).verifyComplete();
            assertThat(calls).hasValue(0);
            StepVerifier.create(authorize(context, HttpMethod.DELETE, "/profile/7", user))
                    .expectError(AccessDeniedException.class).verify();
            assertThat(calls).hasValue(1);
        });
    }

    @Test
    void changedLoginRulesApplyOnTheNextRequest() {
        final var calls = new AtomicInteger();
        runner.withBean(ReactiveAuthorizationManagerProvider.class, () -> provider(calls, false)).run(context -> {
            final var user = new TestingAuthenticationToken("user", "unused", "ROLE_USER");
            StepVerifier.create(authorize(context, HttpMethod.PATCH, "/profile/7", user)).verifyComplete();
            context.getBean(HttpWhiteListProperties.class).setAuthenticated(List.of(new WhiteListRule("GET", "/new")));
            StepVerifier.create(authorize(context, HttpMethod.PATCH, "/profile/7", user))
                    .expectError(AccessDeniedException.class).verify();
            StepVerifier.create(authorize(context, HttpMethod.GET, "/new", user)).verifyComplete();
            assertThat(calls).hasValue(1);
        });
    }

    private ReactiveAuthorizationManagerProvider<AuthorizationContext> provider(AtomicInteger calls, boolean granted) {
        return () -> (authentication, context) -> {
            calls.incrementAndGet();
            return Mono.just(new AuthorizationDecision(granted));
        };
    }

    private Mono<Void> authorize(ApplicationContext context, HttpMethod method, String path, Authentication authentication) {
        final var exchange = MockServerWebExchange.from(MockServerHttpRequest.method(method, path).build());
        final Mono<Void> authorization = context.getBean(SecurityWebFilterChain.class).getWebFilters()
                .filter(AuthorizationWebFilter.class::isInstance).collectList()
                .flatMap(filters -> new DefaultWebFilterChain(ignored -> Mono.empty(), filters).filter(exchange));
        return authentication == null ? authorization
                : authorization.contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
    }
}
