package com.github.zeng.alt.security.core.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.security.api.AuthorizationConstraint;
import com.github.zeng.alt.security.api.AuthorizationManagerProvider;
import com.github.zeng.alt.security.api.WhiteListService;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
            .withBean(ObjectMapper.class, ObjectMapper::new)
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
        return context.getBean(SecurityFilterChain.class).getFilters().stream()
                .filter(AuthorizationFilter.class::isInstance)
                .map(AuthorizationFilter.class::cast)
                .findFirst().orElseThrow().getAuthorizationManager();
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
