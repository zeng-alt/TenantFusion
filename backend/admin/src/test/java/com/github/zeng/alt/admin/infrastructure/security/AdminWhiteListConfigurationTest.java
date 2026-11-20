package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.security.api.WhiteListAccess;
import com.github.zeng.alt.security.api.WhiteListRule;
import com.github.zeng.alt.security.api.WhiteListService;
import com.github.zeng.alt.security.core.WhiteListAutoConfiguration;
import com.github.zeng.alt.security.core.WhiteListTemplate;
import io.vavr.control.Try;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 读取实际 admin 配置，验证七条登录规则与各环境公开范围完整迁移。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AdminWhiteListConfigurationTest {
    @ParameterizedTest
    @CsvSource({
            "POST,/v1/auth/current-role/switch/operator/false",
            "GET,/v1/auth/admin",
            "PATCH,/v1/user/profile/7",
            "POST,/v1/auth/password",
            "GET,/v1/user/detail",
            "GET,/v1/menu/resource/tree",
            "GET,/v1/menu/resource/validate"
    })
    void keepsAllSevenLoginRulesInBothProfiles(String method, String path) {
        Stream.of("dev", "prod").forEach(profile -> runner(profile).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(WhiteListService.class).getAuthenticated()).hasSize(7);
            assertThat(context.getBean(WhiteListTemplate.class).resolve(new MockHttpServletRequest(method, path)))
                    .isEqualTo(WhiteListAccess.AUTHENTICATED);
            assertThat(context.getBean(WhiteListTemplate.class).resolve(new MockHttpServletRequest("DELETE", path)))
                    .isEqualTo(WhiteListAccess.PROTECTED);
        }));
    }

    @Test
    void developmentKeepsExactlyItsExistingEightPublicPaths() {
        runner("dev").run(context -> assertThat(context.getBean(WhiteListService.class).getPermitAll())
                .containsExactlyInAnyOrder(new WhiteListRule("/h2-console/**"), new WhiteListRule("/v1/test/**"),
                        new WhiteListRule("/v1/camunda-identity/**"), new WhiteListRule("/v1/auth/captcha"),
                        new WhiteListRule("/v3/api-docs/**"), new WhiteListRule("/swagger-ui/**"),
                        new WhiteListRule("/swagger-ui.html"), new WhiteListRule("/webjars/**")));
    }

    @Test
    void productionKeepsOnlyCaptchaPublic() {
        runner("prod").run(context -> {
            assertThat(context.getBean(WhiteListService.class).getPermitAll())
                    .containsExactly(new WhiteListRule("/v1/auth/captcha"));
            final WhiteListTemplate template = context.getBean(WhiteListTemplate.class);
            Stream.of("/v1/test/example", "/v3/api-docs/swagger-config", "/v1/camunda-identity/users/admin")
                    .forEach(path -> assertThat(template.resolve(new MockHttpServletRequest("GET", path)))
                            .isEqualTo(WhiteListAccess.PROTECTED));
        });
    }

    private ApplicationContextRunner runner(String profile) {
        return new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(WhiteListAutoConfiguration.class))
                .withInitializer(context -> Try.run(() -> {
                    final var loader = new YamlPropertySourceLoader();
                    loader.load("admin", new ClassPathResource("application.yml"))
                            .forEach(context.getEnvironment().getPropertySources()::addLast);
                    loader.load("profile", new ClassPathResource("application-" + profile + ".yml"))
                            .forEach(context.getEnvironment().getPropertySources()::addFirst);
                }).get());
    }
}
