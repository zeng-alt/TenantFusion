package com.github.zeng.alt.security.abac.client.config;

import com.github.zeng.alt.security.abac.AbacTemplate;
import com.github.zeng.alt.security.abac.AbacRuntimeHints;
import com.github.zeng.alt.security.abac.client.AbacClientCustomizer;
import com.github.zeng.alt.security.abac.client.cache.CachedAbacPolicyLoader;
import com.github.zeng.alt.security.abac.client.listener.AbacPolicyChangeListener;
import com.github.zeng.alt.security.abac.client.listener.AbacPolicySubscriptionListener;
import com.github.zeng.alt.security.abac.core.AbacEvaluator;
import com.github.zeng.alt.security.abac.core.AbacPolicyLoader;
import com.github.zeng.alt.security.abac.core.AbacProperties;
import com.github.zeng.alt.security.abac.core.AbacTemplateCustomizer;
import com.github.zeng.alt.security.abac.core.AbacTemplateImpl;
import com.github.zeng.alt.security.abac.policy.AbacPolicyQueryService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * 客户端拥有授权门面。规则来源可为本地服务或集群 Feign，server-only 部署不创建执行链。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@AutoConfiguration(afterName = {
        "com.github.zeng.alt.security.abac.core.AbacAutoConfiguration",
        "com.github.zeng.alt.security.abac.server.config.AbacServerAutoConfiguration"
})
@EnableConfigurationProperties(AbacClientProperties.class)
@ImportRuntimeHints(AbacRuntimeHints.class)
@ConditionalOnMissingBean(AbacTemplate.class)
@ConditionalOnProperty(prefix = "security.abac.client", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AbacClientAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(AbacPolicyLoader.class)
    public CachedAbacPolicyLoader cachedAbacPolicyLoader(
            AbacPolicyQueryService source,
            AbacEvaluator evaluator,
            AbacClientProperties properties,
            ObjectProvider<AbacClientCustomizer> customizers
    ) {
        customizers.orderedStream().forEach(customizer -> customizer.customize(properties));
        return new CachedAbacPolicyLoader(source, evaluator, properties);
    }

    @Bean
    public AbacTemplate abacTemplate(
            AbacPolicyLoader loader,
            AbacEvaluator evaluator,
            AbacProperties properties,
            ObjectProvider<AbacTemplateCustomizer> customizers
    ) {
        customizers.orderedStream().forEach(customizer -> customizer.customize(properties));
        return new AbacTemplateImpl(loader, evaluator, properties);
    }

    @Bean
    @ConditionalOnBean(CachedAbacPolicyLoader.class)
    public AbacPolicyChangeListener abacPolicyChangeListener(CachedAbacPolicyLoader loader) {
        return new AbacPolicyChangeListener(loader);
    }

    @Bean
    @ConditionalOnBean(CachedAbacPolicyLoader.class)
    @ConditionalOnProperty(prefix = "security.abac.client", name = "notifications-enabled", havingValue = "true")
    public AbacPolicySubscriptionListener abacPolicySubscriptionListener(CachedAbacPolicyLoader loader) {
        return new AbacPolicySubscriptionListener(loader);
    }
}
