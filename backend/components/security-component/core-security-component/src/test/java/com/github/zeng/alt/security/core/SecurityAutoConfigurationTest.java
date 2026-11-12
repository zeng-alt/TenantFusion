package com.github.zeng.alt.security.core;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import reactor.core.publisher.Mono;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证 Servlet 安全组件不会提供可登录的固定缺省账户。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class SecurityAutoConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class));

    @Test
    void shouldNotCreateFixedRootAccount() {
        runner.run(context -> assertThatThrownBy(() -> context.getBean(UserDetailsService.class)
                .loadUserByUsername("root")).isInstanceOf(UsernameNotFoundException.class));
    }

    @Test
    void shouldKeepHostUserDetailsService() {
        final UserDetailsService users = new InMemoryUserDetailsManager();
        runner.withBean(UserDetailsService.class, () -> users)
                .run(context -> assertThat(context.getBean(UserDetailsService.class)).isSameAs(users));
    }

    @Test
    void shouldNotCreateReactiveFixedRootAccount() {
        runner.run(context -> assertThat(context.getBean(ReactiveUserDetailsService.class)
                .findByUsername("root").blockOptional()).isEmpty());
    }

    @Test
    void shouldKeepHostReactiveUserDetailsService() {
        final ReactiveUserDetailsService users = username -> Mono.empty();
        runner.withBean(ReactiveUserDetailsService.class, () -> users)
                .run(context -> assertThat(context.getBean(ReactiveUserDetailsService.class)).isSameAs(users));
    }
}
