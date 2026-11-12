package com.github.zeng.alt.security.rbac.client.collector;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 Spring 7 版本谓词不改变已有方法与路径权限键。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class RouteTemplateCollectorTest {

    @Test
    void servletCollectsVersionedNestedRoutesWithoutDuplicatingPermissionKeys() {
        final var routes = org.springframework.web.servlet.function.RouterFunctions.route()
                .path("/api", builder -> builder
                        .GET("/users/{id}", org.springframework.web.servlet.function.RequestPredicates.version("1.0"),
                                request -> org.springframework.web.servlet.function.ServerResponse.ok().build())
                        .GET("/users/{id}", org.springframework.web.servlet.function.RequestPredicates.version("2.0"),
                                request -> org.springframework.web.servlet.function.ServerResponse.ok().build()))
                .build();

        final var collector = new ServletRouteTemplateCollector(List.of(), List.of(routes));

        assertThat(collector.collectTemplates()).containsExactly("GET:/api/users/{id}");
    }

    @Test
    void reactiveCollectsVersionedNestedRoutesWithoutDuplicatingPermissionKeys() {
        final var routes = org.springframework.web.reactive.function.server.RouterFunctions.route()
                .path("/api", builder -> builder
                        .GET("/users/{id}", org.springframework.web.reactive.function.server.RequestPredicates.version("1.0"),
                                request -> org.springframework.web.reactive.function.server.ServerResponse.ok().build())
                        .GET("/users/{id}", org.springframework.web.reactive.function.server.RequestPredicates.version("2.0"),
                                request -> org.springframework.web.reactive.function.server.ServerResponse.ok().build()))
                .build();

        final var collector = new ReactiveRouteTemplateCollector(List.of(), List.of(routes));

        assertThat(collector.collectTemplates()).containsExactly("GET:/api/users/{id}");
    }
}
