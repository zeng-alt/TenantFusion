package com.github.zeng.alt.security.core;

import com.github.zeng.alt.security.api.WhiteListAccess;
import com.github.zeng.alt.security.api.WhiteListRule;
import com.github.zeng.alt.security.api.WhiteListService;
import com.github.zeng.alt.security.core.properties.HttpWhiteListProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证白名单配置绑定、旧配置兼容及宿主的有序贡献。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class WhiteListAutoConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(WhiteListAutoConfiguration.class));

    @Test
    void bindsBothGroupsAndMergesLegacyPublicPaths() {
        runner.withPropertyValues("security.filter.ignore-url[0]=/legacy/**",
                "security.whitelist.permit-all[0].method=get", "security.whitelist.permit-all[0].path=/public",
                "security.whitelist.authenticated[0].method=PATCH",
                "security.whitelist.authenticated[0].path=/profile/{id}").run(context -> {
                    assertThat(context).hasNotFailed();
                    final WhiteListService service = context.getBean(WhiteListService.class);
                    assertThat(service.getPermitAll()).containsExactlyInAnyOrder(
                            new WhiteListRule("/legacy/**"), new WhiteListRule("GET", "/public"));
                    assertThat(service.getAuthenticated()).containsExactly(new WhiteListRule("PATCH", "/profile/{id}"));
                    final WhiteListTemplate template = context.getBean(WhiteListTemplate.class);
                    assertThat(template.resolve(new MockHttpServletRequest("POST", "/legacy/test")))
                            .isEqualTo(WhiteListAccess.PERMIT_ALL);
                    assertThat(template.resolve(new MockHttpServletRequest("POST", "/public")))
                            .isEqualTo(WhiteListAccess.PROTECTED);
                });
    }

    @Test
    void omittedMethodMatchesAllMethods() {
        runner.withPropertyValues("security.whitelist.permit-all[0].path=/assets/**").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(WhiteListTemplate.class).resolve(new MockHttpServletRequest("HEAD", "/assets/a")))
                    .isEqualTo(WhiteListAccess.PERMIT_ALL);
        });
    }

    @Test
    void keepsLegacyHostServiceAndAppliesOrderedCustomizers() {
        final WhiteListService service = () -> Set.of("/host");
        runner.withBean(WhiteListService.class, () -> service).withUserConfiguration(Contributors.class).run(context -> {
            assertThat(context.getBean(WhiteListService.class)).isSameAs(service);
            assertThat(context.getBean(Contributors.class).calls).containsExactly("first", "last");
            final WhiteListTemplate template = context.getBean(WhiteListTemplate.class);
            assertThat(template.resolve(new MockHttpServletRequest("GET", "/host")))
                    .isEqualTo(WhiteListAccess.PERMIT_ALL);
            assertThat(template.resolve(new MockHttpServletRequest("GET", "/contributed")))
                    .isEqualTo(WhiteListAccess.AUTHENTICATED);
        });
    }

    @Test
    void rejectsIdenticalRuleInBothGroupsAtStartup() {
        runner.withPropertyValues("security.whitelist.permit-all[0].method=GET",
                "security.whitelist.permit-all[0].path=/conflict",
                "security.whitelist.authenticated[0].method=get",
                "security.whitelist.authenticated[0].path=/conflict").run(context ->
                assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class)
                        .hasStackTraceContaining("公开白名单与登录白名单规则冲突"));
    }

    @Test
    void permitsDifferentMethodsForTheSamePath() {
        runner.withPropertyValues("security.whitelist.permit-all[0].method=GET",
                "security.whitelist.permit-all[0].path=/same",
                "security.whitelist.authenticated[0].method=POST",
                "security.whitelist.authenticated[0].path=/same").run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void invalidPatternFailsAtStartup() {
        runner.withPropertyValues("security.whitelist.permit-all[0].path=/broken/**/tail")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void changesToBothGroupsApplyOnTheNextRequest() {
        runner.withPropertyValues("security.whitelist.permit-all[0].path=/old-public",
                "security.whitelist.authenticated[0].path=/old-login").run(context -> {
            final WhiteListTemplate template = context.getBean(WhiteListTemplate.class);
            final HttpWhiteListProperties properties = context.getBean(HttpWhiteListProperties.class);
            assertThat(template.resolve(new MockHttpServletRequest("GET", "/old-login")))
                    .isEqualTo(WhiteListAccess.AUTHENTICATED);
            properties.setPermitAll(List.of(new WhiteListRule("/new-public")));
            properties.setAuthenticated(List.of(new WhiteListRule("/new-login")));
            assertThat(template.resolve(new MockHttpServletRequest("GET", "/old-public")))
                    .isEqualTo(WhiteListAccess.PROTECTED);
            assertThat(template.resolve(new MockHttpServletRequest("GET", "/old-login")))
                    .isEqualTo(WhiteListAccess.PROTECTED);
            assertThat(template.resolve(new MockHttpServletRequest("GET", "/new-public")))
                    .isEqualTo(WhiteListAccess.PERMIT_ALL);
            assertThat(template.resolve(new MockHttpServletRequest("GET", "/new-login")))
                    .isEqualTo(WhiteListAccess.AUTHENTICATED);
        });
    }

    @Test
    void classificationUsesContextRelativePathAndUpdatesForForwardedTarget() {
        runner.withPropertyValues("security.whitelist.permit-all[0].path=/public").run(context -> {
            final WhiteListTemplate template = context.getBean(WhiteListTemplate.class);
            final var request = new MockHttpServletRequest("GET", "/app/public");
            request.setContextPath("/app");
            assertThat(template.resolve(request)).isEqualTo(WhiteListAccess.PERMIT_ALL);
            request.setRequestURI("/app/private");
            assertThat(template.resolve(request)).isEqualTo(WhiteListAccess.PROTECTED);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class Contributors {
        private final List<String> calls = new ArrayList<>();

        @Bean
        @Order(10)
        WhiteListCustomizer lastContributor() {
            return template -> {
                calls.add("last");
                template.authenticated(HttpMethod.GET, "/contributed");
            };
        }

        @Bean
        @Order(-10)
        WhiteListCustomizer firstContributor() {
            return template -> {
                calls.add("first");
                template.permitAll("/contributed-public");
            };
        }
    }
}
