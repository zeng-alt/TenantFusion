package com.github.zeng.alt.security.jwt;

import com.github.zeng.alt.security.api.SecurityUser;
import jakarta.validation.Validation;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 校验解码后的 HMAC 位数，覆盖签发与仅验证配置。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class JwtPropertiesValidationTest {
    @Test
    void acceptsMinimumAccessAndRefreshKeysAndSignsBothTokens() {
        final var properties = properties(32, 64);
        assertValid(properties);
        final var tokens = new JwtTokenProvider(properties);
        final var user = SecurityUser.withUsername("review").id("7").tenant("a").password("unused").build();
        assertThat(tokens.parseAccessToken(tokens.createToken(user)).result())
                .isEqualTo(JwtTokenProvider.TokenValidationResult.VALID);
        assertThat(tokens.validateRefreshToken(tokens.createRefreshToken(user))).isTrue();
    }

    @Test
    void accepts256BitAccessOnlyKeyButRequires512BitsWhenReusingForRefresh() {
        final var properties = properties(32, null);
        assertValid(properties);
        properties.setAuthentication(true);
        assertInvalid(properties);
        properties.setBase64Secret(key(64));
        assertValid(properties);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not-base64!", "YWJj", "                                                                "})
    void rejectsInvalidAccessKeys(String secret) {
        final var properties = properties(32, 64);
        properties.setBase64Secret(secret);
        assertInvalid(properties);
    }

    @Test
    void rejectsShortRefreshKey() {
        assertInvalid(properties(32, 63));
        assertInvalid(properties(31, 64));
    }

    @Test
    void bindsMinimumKeysThroughSpringConfigurationValidation() {
        new ApplicationContextRunner().withUserConfiguration(PropertiesConfiguration.class)
                .withPropertyValues("security.jwt-auth.authentication=true",
                        "security.jwt-auth.base64-secret=" + key(32),
                        "security.jwt-auth.base64-secret-refresh=" + key(64))
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void rejectsMalformedRefreshKeyDuringSpringBinding() {
        new ApplicationContextRunner().withUserConfiguration(PropertiesConfiguration.class)
                .withPropertyValues("security.jwt-auth.base64-secret=" + key(64),
                        "security.jwt-auth.base64-secret-refresh=not-base64!")
                .run(context -> assertThat(context).hasFailed());
    }

    private JwtProperties properties(int accessBytes, Integer refreshBytes) {
        final var properties = new JwtProperties();
        properties.setBase64Secret(key(accessBytes));
        properties.setBase64SecretRefresh(refreshBytes == null ? null : key(refreshBytes));
        return properties;
    }

    private String key(int bytes) {
        return Base64.getEncoder().encodeToString(new byte[bytes]);
    }

    private void assertValid(JwtProperties properties) {
        try (final var factory = Validation.byDefaultProvider().configure()
                .messageInterpolator(new ParameterMessageInterpolator()).buildValidatorFactory()) {
            assertThat(factory.getValidator().validate(properties)).isEmpty();
        }
    }

    private void assertInvalid(JwtProperties properties) {
        try (final var factory = Validation.byDefaultProvider().configure()
                .messageInterpolator(new ParameterMessageInterpolator()).buildValidatorFactory()) {
            assertThat(factory.getValidator().validate(properties)).isNotEmpty();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(JwtProperties.class)
    static class PropertiesConfiguration { }
}
