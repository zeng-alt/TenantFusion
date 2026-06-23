package com.github.zeng.alt.security.api;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.List;

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
}
