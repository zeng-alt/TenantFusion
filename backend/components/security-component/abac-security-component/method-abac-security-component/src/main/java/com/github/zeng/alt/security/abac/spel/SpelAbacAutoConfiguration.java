package com.github.zeng.alt.security.abac.spel;

import com.github.zeng.alt.security.abac.core.AbacEvaluator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * SpEL 技术实现的自动装配。先贡献求值器，再由 client 组装授权门面。
 * 自定义求值器 Bean 存在时退让，不要求替换整个授权门面。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@AutoConfiguration(beforeName = "com.github.zeng.alt.security.abac.client.config.AbacClientAutoConfiguration")
@EnableConfigurationProperties(AbacSpelProperties.class)
public class SpelAbacAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(AbacEvaluator.class)
    public SpelAbacEvaluator abacEvaluator(AbacSpelProperties properties,
            ObjectProvider<AbacFunctionResolver> functions) {
        return new SpelAbacEvaluator(new SpelExpressionParser(), properties, functions.orderedStream().toList());
    }
}
