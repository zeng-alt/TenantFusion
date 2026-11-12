package com.github.zeng.alt.security.abac.method;

import com.github.zeng.alt.security.abac.AbacTemplate;
import com.github.zeng.alt.security.abac.core.AbacAutoConfiguration;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 方法级 ABAC 自动装配。先贡献注册表与拦截器，再包装为 Advisor；宿主必须提供主体与环境 SPI，
 * 缺失时启动失败而非静默禁用。自定义 Advisor 存在时退让。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@AutoConfiguration(after = AbacAutoConfiguration.class)
@EnableConfigurationProperties(AbacMethodProperties.class)
@ConditionalOnProperty(
        prefix = "security.abac.method",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class AbacMethodAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AbacAuthorizeAttributeRegistry abacAuthorizeAttributeRegistry() {
        return new AbacAuthorizeAttributeRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public AbacMethodInterceptor abacMethodInterceptor(
            AbacAuthorizeAttributeRegistry registry,
            AbacSubjectProvider subjectProvider,
            AbacEnvironmentProvider environmentProvider,
            ListableBeanFactory beanFactory,
            AbacTemplate template
    ) {
        return new AbacMethodInterceptor(registry, subjectProvider, environmentProvider, beanFactory, template);
    }

    @Bean
    @ConditionalOnMissingBean
    public AbacMethodAdvisor abacMethodAdvisor(
            ObjectProvider<AbacMethodInterceptor> interceptor, AbacMethodProperties properties) {
        return new AbacMethodAdvisor(interceptor, properties.getOrder());
    }
}