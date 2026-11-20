package com.github.zeng.alt.security.abac.method;

import com.github.zeng.alt.security.abac.AbacTemplate;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import com.github.zeng.alt.security.abac.AbacResourceResolver;
import com.github.zeng.alt.security.abac.spel.SpelAbacEvaluator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 方法级 ABAC 自动装配。先贡献注册表与拦截器，再包装为 Advisor；宿主必须提供主体与环境 SPI，
 * 缺失时启动失败而非静默禁用。自定义 Advisor 存在时退让。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@AutoConfiguration(afterName = "com.github.zeng.alt.security.abac.client.config.AbacClientAutoConfiguration")
@EnableConfigurationProperties(AbacMethodProperties.class)
@EnableTransactionManagement
@ConditionalOnBooleanProperty(prefix = "security.abac.client", name = "enabled", matchIfMissing = true)
@ConditionalOnProperty(
        prefix = "security.abac.method",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class AbacMethodAutoConfiguration {

    @Bean
    public static BeanFactoryInitializationAotProcessor abacMethodAotProcessor() {
        return new AbacMethodAotProcessor();
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static BeanPostProcessor abacTransactionAdvisorHandler(ObjectProvider<AbacMethodProperties> properties) {
        return new AbacTransactionAdvisorHandler(properties);
    }

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
    public AbacMethodResolver abacMethodResolver(ConfigurableListableBeanFactory beans,
            AbacAuthorizeAttributeRegistry registry, ObjectProvider<AbacResourceResolver> resources,
            ObjectProvider<AbacSchemaCustomizer> customizers, ObjectProvider<SpelAbacEvaluator> evaluator) {
        return new AbacMethodResolver(beans, registry, resources, customizers, evaluator);
    }

    @Bean
    @ConditionalOnMissingBean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public AbacMethodAdvisor abacMethodAdvisor(
            ObjectProvider<AbacMethodInterceptor> interceptor, AbacMethodProperties properties) {
        return new AbacMethodAdvisor(interceptor, properties.getOrder());
    }
}
