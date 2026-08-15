package com.github.zeng.alt.camunda.identity.api.config;

import com.github.zeng.alt.camunda.identity.api.CamundaIdentityProvider;
import com.github.zeng.alt.camunda.identity.api.CamundaIdentityProviderPlugin;
import com.github.zeng.alt.camunda.identity.api.CamundaTenantSource;
import com.github.zeng.alt.camunda.identity.api.CamundaUserGroupSource;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Camunda 自定义身份提供者自动配置。
 * <p>
 * 当容器中存在 {@link CamundaUserGroupSource} SPI 实现时，自动装配只读身份提供者
 * 并注册为 {@link ProcessEnginePlugin}。
 */
@AutoConfiguration
@ConditionalOnClass(ProcessEnginePlugin.class)
@ConditionalOnBean(CamundaUserGroupSource.class)
public class CamundaIdentityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CamundaIdentityProvider camundaIdentityProvider(
            CamundaUserGroupSource source,
            ObjectProvider<CamundaTenantSource> tenantSourceProvider) {
        return new CamundaIdentityProvider(source, tenantSourceProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    public CamundaIdentityProviderPlugin camundaIdentityProviderPlugin(CamundaIdentityProvider provider) {
        return new CamundaIdentityProviderPlugin(provider);
    }
}
