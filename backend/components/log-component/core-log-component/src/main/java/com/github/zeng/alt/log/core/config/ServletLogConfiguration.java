package com.github.zeng.alt.log.core.config;

import com.github.zeng.alt.log.core.support.IpResolver;
import com.github.zeng.alt.log.core.support.RequestResolver;
import com.github.zeng.alt.log.core.support.ServletIpResolver;
import com.github.zeng.alt.log.core.support.ServletRequestResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 仅在 Servlet 应用中装配 HTTP 请求解析。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ServletLogConfiguration {
    @Bean
    @ConditionalOnMissingBean(IpResolver.class)
    public IpResolver servletIpResolver() {
        return new ServletIpResolver();
    }

    @Bean
    @ConditionalOnMissingBean(RequestResolver.class)
    public RequestResolver servletRequestResolver() {
        return new ServletRequestResolver();
    }
}
