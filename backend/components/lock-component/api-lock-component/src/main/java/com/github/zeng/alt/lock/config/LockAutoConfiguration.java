package com.github.zeng.alt.lock.config;

import com.github.zeng.alt.lock.LockRuntimeHints;
import com.github.zeng.alt.lock.MethodBasedExpressionEvaluator;
import com.github.zeng.alt.lock.SpelMethodBasedExpressionEvaluator;
import com.github.zeng.alt.lock.aop.LockAnnotationAdvisor;
import com.github.zeng.alt.lock.aop.LockInterceptor;
import com.github.zeng.alt.lock.api.LockTemplate;
import com.github.zeng.alt.lock.api.NoOpLockTemplate;
import com.github.zeng.alt.lock.model.DefaultLockFailureStrategy;
import com.github.zeng.alt.lock.model.DefaultLockKeyBuilder;
import com.github.zeng.alt.lock.model.LockFailureStrategy;
import com.github.zeng.alt.lock.model.LockKeyBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;

import java.util.List;

/**
 * Lock module auto-configuration
 *
 * @author zengJiaJun
 * @since 2026年06月09日
 *
 * @version 1.0
 */
@AutoConfiguration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@EnableConfigurationProperties(LockProperties.class)
@ImportRuntimeHints(LockRuntimeHints.class)
public class LockAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MethodBasedExpressionEvaluator methodBasedExpressionEvaluator() {
        return new SpelMethodBasedExpressionEvaluator();
    }

    @Bean
    @ConditionalOnMissingBean(LockKeyBuilder.class)
    public DefaultLockKeyBuilder defaultLockKeyBuilder(
            MethodBasedExpressionEvaluator expressionEvaluator) {
        return new DefaultLockKeyBuilder(expressionEvaluator);
    }

    @Bean
    @ConditionalOnMissingBean(LockFailureStrategy.class)
    public DefaultLockFailureStrategy defaultLockFailureStrategy() {
        return new DefaultLockFailureStrategy();
    }

    @Bean
    @ConditionalOnMissingBean(LockTemplate.class)
    public LockTemplate noOpLockTemplate() {
        return new NoOpLockTemplate();
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean
    public LockInterceptor lockInterceptor(
            ObjectProvider<LockTemplate> lockTemplate,
            ObjectProvider<List<LockKeyBuilder>> keyBuilders,
            ObjectProvider<List<LockFailureStrategy>> failureStrategies,
            ObjectProvider<LockProperties> lockProperties,
            ObjectProvider<MethodBasedExpressionEvaluator> expressionEvaluator) {

        return new LockInterceptor(
                lockTemplate,
                keyBuilders,
                failureStrategies,
                lockProperties,
                expressionEvaluator
        );
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean
    public LockAnnotationAdvisor lockAnnotationAdvisor(LockInterceptor lockInterceptor) {
        return new LockAnnotationAdvisor(lockInterceptor, Ordered.HIGHEST_PRECEDENCE);
    }
}
