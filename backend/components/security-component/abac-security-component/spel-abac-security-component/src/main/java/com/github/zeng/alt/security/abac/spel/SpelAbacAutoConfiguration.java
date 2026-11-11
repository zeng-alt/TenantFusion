package com.github.zeng.alt.security.abac.spel;

import com.github.zeng.alt.security.abac.core.AbacAutoConfiguration;
import com.github.zeng.alt.security.abac.core.AbacEvaluator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * SpEL 技术实现的自动装配。先贡献求值器，再由 core 组装授权门面。
 * 自定义求值器 Bean 存在时退让，不要求替换整个授权门面。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@AutoConfiguration(before = AbacAutoConfiguration.class)
public class SpelAbacAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(AbacEvaluator.class)
    public AbacEvaluator abacEvaluator() {
        return new SpelAbacEvaluator();
    }
}
