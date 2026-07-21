package com.github.zeng.alt.security.api;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;

import java.util.List;
import java.util.Optional;

/**
 * Security API 自动配置.
 *
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年10月07日
 */
@AutoConfiguration
public class SecurityApiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LoginHelperFactory loginHelperFactory(List<LoginHelper> helpers) {
        return new LoginHelperFactory(helpers);
    }

    @AutoConfiguration
    @ConditionalOnClass(AuditorAware.class)
    public static class AuditorConfiguration {

        @Bean
        @ConditionalOnMissingBean(AuditorAware.class)
        public AuditorAware<String> auditorAware() {
            return () -> Optional.ofNullable(UserContextHolder.getId());
        }

    }

}
