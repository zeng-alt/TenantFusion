package com.github.zeng.alt.security.abac.server.security;

import com.github.zeng.alt.security.abac.server.config.AbacServerProperties;
import com.github.zeng.alt.security.core.web.SecurityBuilderCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;

/**
 * 使用宿主安全链的窄扩展点接入内部服务身份，不创建公开白名单或替换安全链。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class AbacServiceSecurityAutoConfiguration {
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ConditionalOnMissingBean(name = "abacServiceSecurityCustomizer")
    public SecurityBuilderCustomizer abacServiceSecurityCustomizer(
            AbacServerProperties properties,
            ObjectProvider<AuthenticationEntryPoint> entryPoints,
            ObjectProvider<AccessDeniedHandler> deniedHandlers
    ) {
        return http -> http.addFilterBefore(new AbacServiceAuthenticationFilter(properties,
                        entryPoints.getIfAvailable(() -> new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)),
                        deniedHandlers.getIfAvailable(AccessDeniedHandlerImpl::new)),
                ExceptionTranslationFilter.class);
    }

    @Bean
    @ConditionalOnMissingBean(AbacServiceAuthorizationManager.class)
    public AbacServiceAuthorizationManager abacServiceAuthorizationManager() {
        return new AbacServiceAuthorizationManager();
    }
}
