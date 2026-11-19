package com.github.zeng.alt.security.core.web.config;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.github.zeng.alt.security.api.AuthorizationConstraint;
import com.github.zeng.alt.security.api.AuthorizationManagerProvider;
import com.github.zeng.alt.security.api.WhiteListService;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import com.github.zeng.alt.security.core.web.AuthorizeHttpRequestsCustomizer;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 Servlet 自动配置分别装配 OR 授权与有序 AND 约束。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class WebSecurityAutoConfigurationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(WebSecurityAutoConfiguration.class))
            .withPropertyValues("security.username-login.enabled=false")
            .withBean(SecurityProperties.class, SecurityProperties::new)
            .withBean(ObjectMapper.class, () -> JsonMapper.builder().build())
            .withBean("mvcHandlerMappingIntrospector", HandlerMappingIntrospector.class, HandlerMappingIntrospector::new)
            .withBean(AuthenticationManager.class, () -> authentication -> authentication)
            .withBean(WhiteListService.class, () -> () -> Set.of("/public"));

    @Test
    void withoutConstraintsKeepsWhitelistAndAuthenticatedFallback() {
        runner.run(context -> {
            final var manager = authorizationManager(context);
            assertThat(manager.authorize(() -> null, request("/public")).isGranted()).isTrue();
            assertThat(manager.authorize(() -> null, request("/secured")).isGranted()).isFalse();
            assertThat(manager.authorize(
                    () -> new TestingAuthenticationToken("user", "unused", "ROLE_USER"), request("/secured")
            ).isGranted()).isTrue();
        });
    }

    @Test
    void constraintsVetoWhitelistInOrderAndShortCircuit() {
        runner.withUserConfiguration(OrderedConstraints.class).run(context -> {
            assertThat(authorizationManager(context).authorize(() -> null, request("/public")).isGranted()).isFalse();
            assertThat(context.getBean(OrderedConstraints.class).calls).containsExactly("first", "deny");
        });
    }

    @Test
    void constraintsVetoAuthenticatedFallback() {
        runner.withUserConfiguration(OrderedConstraints.class).run(context -> {
            assertThat(authorizationManager(context).authorize(
                    () -> new TestingAuthenticationToken("user", "unused", "ROLE_USER"), request("/secured")
            ).isGranted()).isFalse();
            assertThat(context.getBean(OrderedConstraints.class).calls).containsExactly("first", "deny");
        });
    }

    @Test
    void constraintsCannotGrantWhenBaseDenies() {
        runner.withUserConfiguration(OrderedConstraints.class).run(context -> {
            assertThat(authorizationManager(context).authorize(() -> null, request("/secured")).isGranted()).isFalse();
            assertThat(context.getBean(OrderedConstraints.class).calls).isEmpty();
        });
    }

    @Test
    void providersRemainOrderedOrAlternatives() {
        runner.withUserConfiguration(OrderedProviders.class).run(context -> {
            assertThat(authorizationManager(context).authorize(() -> null, request("/secured")).isGranted()).isTrue();
            assertThat(context.getBean(OrderedProviders.class).calls).containsExactly("deny", "allow");
        });
    }

    @Test
    void providerGrantStillRequiresConstraints() {
        runner.withUserConfiguration(OrderedProviders.class, OrderedConstraints.class).run(context -> {
            assertThat(authorizationManager(context).authorize(() -> null, request("/secured")).isGranted()).isFalse();
            assertThat(context.getBean(OrderedProviders.class).calls).containsExactly("deny", "allow");
            assertThat(context.getBean(OrderedConstraints.class).calls).containsExactly("first", "deny");
        });
    }

    @Test
    void constraintsVetoCustomAuthenticatedAndPublicPaths() {
        runner.withUserConfiguration(OrderedConstraints.class)
                .withBean(AuthorizeHttpRequestsCustomizer.class, () -> registry -> registry
                        .requestMatchers("/custom/private").authenticated()
                        .requestMatchers("/custom/public").permitAll())
                .run(context -> {
                    final var auth = new TestingAuthenticationToken("user", "unused", "ROLE_USER");
                    assertThat(authorizationManager(context).authorize(
                            () -> auth, request("/custom/private")).isGranted()).isFalse();
                    assertThat(authorizationManager(context).authorize(
                            () -> null, request("/custom/public")).isGranted()).isFalse();
                });
    }

    @Test
    void whitelistChangesApplyToTheNextRequest() {
        final var paths = new AtomicReference<>(Set.of("/public"));
        runner.withAllowBeanDefinitionOverriding(true)
                .withBean(WhiteListService.class, () -> paths::get).run(context -> {
                    final var manager = authorizationManager(context);
                    assertThat(manager.authorize(() -> null, request("/public")).isGranted()).isTrue();
                    paths.set(Set.of("/new-public"));
                    assertThat(manager.authorize(() -> null, request("/public")).isGranted()).isFalse();
                    assertThat(manager.authorize(() -> null, request("/new-public")).isGranted()).isTrue();
                });
    }

    @Test
    void globalConstraintDenialStopsCustomRouteInTheActualFilterChain() {
        runner.withUserConfiguration(OrderedConstraints.class)
                .withBean(AuthorizeHttpRequestsCustomizer.class,
                        () -> registry -> registry.requestMatchers("/custom").authenticated())
                .run(context -> {
                    final var response = new MockHttpServletResponse();
                    final var reached = new AtomicBoolean();
                    final var request = request("/custom");
                    request.getSession().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                            new SecurityContextImpl(new TestingAuthenticationToken("user", "unused", "ROLE_USER")));
                    try {
                        new FilterChainProxy(context.getBean(SecurityFilterChain.class)).doFilter(
                                request, response, (req, res) -> reached.set(true));
                        assertThat(response.getStatus()).isEqualTo(403);
                        assertThat(reached).isFalse();
                        assertThat(context.getBean(OrderedConstraints.class).calls).containsExactly("first", "deny");
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                });
    }

    @ParameterizedTest
    @CsvSource({"GET,/h2-console/index", "POST,/tenant/graphql/query", "GET,/tenant/graphiql/index",
            "POST,/actuator/startup", "POST,/login/unrelated"})
    void shouldNotPermitApplicationPathsByDefault(String method, String path) {
        runner.run(context -> {
            final var request = request(path);
            request.setMethod(method);
            assertThat(authorizationManager(context).authorize(() -> null, request).isGranted()).isFalse();
        });
    }

    private AuthorizationManager<HttpServletRequest> authorizationManager(ApplicationContext context) {
        final var filters = context.getBean(SecurityFilterChain.class).getFilters().stream()
                .filter(AuthorizationFilter.class::isInstance)
                .map(AuthorizationFilter.class::cast)
                .toList();
        return (authentication, request) -> new AuthorizationDecision(filters.stream().allMatch(filter ->
                filter.getAuthorizationManager().authorize(authentication, request).isGranted()));
    }

    private MockHttpServletRequest request(String path) {
        final var request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        return request;
    }

    @Configuration(proxyBeanMethods = false)
    static class OrderedConstraints {
        private final List<String> calls = new ArrayList<>();

        @Bean
        @Order(20)
        AuthorizationConstraint<RequestAuthorizationContext> lastConstraint() {
            return (authentication, context) -> {
                calls.add("unreached");
                return new AuthorizationDecision(true);
            };
        }

        @Bean
        @Order(10)
        AuthorizationConstraint<RequestAuthorizationContext> denyingConstraint() {
            return (authentication, context) -> {
                calls.add("deny");
                return new AuthorizationDecision(false);
            };
        }

        @Bean
        @Order(-10)
        AuthorizationConstraint<RequestAuthorizationContext> firstConstraint() {
            return (authentication, context) -> {
                calls.add("first");
                return new AuthorizationDecision(true);
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class OrderedProviders {
        private final List<String> calls = new ArrayList<>();

        @Bean
        @Order(10)
        AuthorizationManagerProvider<RequestAuthorizationContext> allowingProvider() {
            return () -> (authentication, context) -> {
                calls.add("allow");
                return new AuthorizationDecision(true);
            };
        }

        @Bean
        @Order(-10)
        AuthorizationManagerProvider<RequestAuthorizationContext> denyingProvider() {
            return () -> (authentication, context) -> {
                calls.add("deny");
                return new AuthorizationDecision(false);
            };
        }
    }
}
