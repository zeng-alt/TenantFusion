package com.github.zeng.alt.security.abac.server.config;

import com.github.zeng.alt.event.EventTemplate;
import com.github.zeng.alt.security.abac.core.AbacEvaluator;
import com.github.zeng.alt.security.abac.AbacRuntimeHints;
import com.github.zeng.alt.security.abac.policy.AbacPolicyQueryService;
import com.github.zeng.alt.security.abac.server.AbacPolicyService;
import com.github.zeng.alt.security.abac.server.event.AbacPolicyEventListener;
import com.github.zeng.alt.security.abac.server.service.AbacPolicyQueryServiceImpl;
import com.github.zeng.alt.security.abac.server.service.AbacPolicyServiceImpl;
import com.github.zeng.alt.security.abac.server.spi.AbacPolicyRepository;
import com.github.zeng.alt.security.abac.server.spi.AbacPolicyStore;
import com.github.zeng.alt.security.abac.server.web.AbacPolicyController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import com.github.zeng.alt.security.abac.server.security.AbacServiceSecurityAutoConfiguration;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * 独立策略服务端装配，不依赖客户端，也不强制创建授权门面或方法 Advisor。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@AutoConfiguration
@EnableConfigurationProperties(AbacServerProperties.class)
@Import(AbacServiceSecurityAutoConfiguration.class)
@ImportRuntimeHints(AbacRuntimeHints.class)
@ConditionalOnProperty(prefix = "security.abac.server", name = "enabled", havingValue = "true")
public class AbacServerAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(AbacPolicyQueryService.class)
    public AbacPolicyQueryService abacPolicyQueryService(AbacPolicyRepository repository) {
        return new AbacPolicyQueryServiceImpl(repository);
    }

    @Bean
    @ConditionalOnMissingBean(AbacPolicyService.class)
    @ConditionalOnBean(AbacPolicyStore.class)
    public AbacPolicyService abacPolicyService(
            AbacPolicyStore store, AbacEvaluator evaluator, ApplicationEventPublisher events) {
        return new AbacPolicyServiceImpl(store, evaluator, events);
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public AbacPolicyController abacPolicyController(AbacPolicyQueryService queries) {
        return new AbacPolicyController(queries);
    }

    @Bean
    @ConditionalOnProperty(prefix = "security.abac.server", name = "notifications-enabled", havingValue = "true")
    public AbacPolicyEventListener abacPolicyEventListener(EventTemplate events, AbacServerProperties properties) {
        return new AbacPolicyEventListener(events, properties);
    }
}
