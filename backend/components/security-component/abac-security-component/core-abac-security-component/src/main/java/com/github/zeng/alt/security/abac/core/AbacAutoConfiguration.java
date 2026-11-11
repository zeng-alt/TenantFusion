package com.github.zeng.alt.security.abac.core;

import com.github.zeng.alt.security.abac.AbacTemplate;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * ABAC 公共自动装配。必须提供策略加载器及求值器，缺失时启动失败而非静默禁用。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@AutoConfiguration
@EnableConfigurationProperties(AbacProperties.class)
public class AbacAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(AbacTemplate.class)
    public AbacTemplate abacTemplate(
            AbacPolicyLoader loader,
            AbacEvaluator evaluator,
            AbacProperties properties,
            ObjectProvider<AbacTemplateCustomizer> customizers
    ) {
        customizers.orderedStream().forEach(customizer -> customizer.customize(properties));
        return new AbacTemplateImpl(loader, evaluator, properties);
    }
}
