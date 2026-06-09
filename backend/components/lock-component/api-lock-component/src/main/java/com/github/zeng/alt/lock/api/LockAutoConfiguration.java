package com.github.zeng.alt.lock.api;

import com.github.zeng.alt.lock.MethodBasedExpressionEvaluator;
import com.github.zeng.alt.lock.SpelMethodBasedExpressionEvaluator;
import com.github.zeng.alt.lock.aop.LockAnnotationAdvisor;
import com.github.zeng.alt.lock.aop.LockInterceptor;
import com.github.zeng.alt.lock.model.DefaultLockFailureStrategy;
import com.github.zeng.alt.lock.model.DefaultLockKeyBuilder;
import com.github.zeng.alt.lock.model.LockFailureStrategy;
import com.github.zeng.alt.lock.model.LockKeyBuilder;
import com.github.zeng.alt.lock.model.LockProperties;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;

import java.util.List;

/**
 * 锁模块自动配置
 *
 * @author zengJiaJun
 * @since 2026年06月09日
 * @version 1.0
 */
@Configuration
@EnableConfigurationProperties(LockProperties.class)
public class LockAutoConfiguration {

    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @Bean
    @ConditionalOnMissingBean
    public MethodBasedExpressionEvaluator methodBasedExpressionEvaluator() {
        return new SpelMethodBasedExpressionEvaluator();
    }

    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @Bean
    @ConditionalOnMissingBean(LockKeyBuilder.class)
    public DefaultLockKeyBuilder defaultLockKeyBuilder(
            MethodBasedExpressionEvaluator expressionEvaluator) {
        return new DefaultLockKeyBuilder(expressionEvaluator);
    }

    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @Bean
    @ConditionalOnMissingBean(LockFailureStrategy.class)
    public DefaultLockFailureStrategy defaultLockFailureStrategy() {
        return new DefaultLockFailureStrategy();
    }

    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @Bean
    @ConditionalOnMissingBean(LockTemplate.class)
    public LockTemplate noOpLockTemplate() {
        return new NoOpLockTemplate();
    }

    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @Bean
    @ConditionalOnMissingBean
    public LockInterceptor lockInterceptor(
            LockTemplate lockTemplate,
            List<LockKeyBuilder> keyBuilders,
            List<LockFailureStrategy> failureStrategies,
            LockProperties lockProperties,
            MethodBasedExpressionEvaluator expressionEvaluator) {
        return new LockInterceptor(
                lockTemplate, keyBuilders, failureStrategies,
                lockProperties, expressionEvaluator);
    }

    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @Bean
    @ConditionalOnMissingBean
    public LockAnnotationAdvisor lockAnnotationAdvisor(LockInterceptor lockInterceptor) {
        return new LockAnnotationAdvisor(lockInterceptor, Ordered.HIGHEST_PRECEDENCE);
    }
}
