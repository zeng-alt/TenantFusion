package com.github.zeng.alt.camunda.identity.api.security;

import com.github.zeng.alt.camunda.identity.api.CamundaUserGroupSource;
import com.github.zeng.alt.security.core.web.SecurityBuilderCustomizer;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 将当前认证用户同步到 Camunda {@link IdentityService} 的自动配置。
 * <p>
 * 与 {@code JwtAuthenticationFilter} 的方式一致，通过 {@link SecurityBuilderCustomizer}
 * 在 Spring Security 过滤链中加入 {@link CamundaIdentityAuthenticationFilter}。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({ProcessEnginePlugin.class, IdentityService.class})
@ConditionalOnBean({CamundaUserGroupSource.class, IdentityService.class})
@AutoConfigureAfter(com.github.zeng.alt.camunda.identity.api.config.CamundaIdentityAutoConfiguration.class)
public class CamundaIdentitySecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CamundaIdentityAuthenticationFilter camundaIdentityAuthenticationFilter(IdentityService identityService) {
        return new CamundaIdentityAuthenticationFilter(identityService);
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityBuilderCustomizer camundaIdentitySecurityCustomizer(
            CamundaIdentityAuthenticationFilter camundaIdentityAuthenticationFilter) {
        return http -> http.addFilterAfter(camundaIdentityAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }
}