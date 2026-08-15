package com.github.zeng.alt.camunda.identity.api;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;

/**
 * 将自定义 {@link CamundaIdentityProvider} 注册为 Camunda 引擎的身份提供者插件。
 * <p>
 * 在引擎 preInit 阶段替换默认的 DB identity provider，使登录、任务候选人等
 * 查询走 admin 的用户/角色体系。
 */
public class CamundaIdentityProviderPlugin implements ProcessEnginePlugin {

    private final CamundaIdentityProvider provider;

    public CamundaIdentityProviderPlugin(CamundaIdentityProvider provider) {
        this.provider = provider;
    }

    @Override
    public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        processEngineConfiguration.setIdentityProviderSessionFactory(
                new CamundaIdentityProviderFactory(provider));
    }

    @Override
    public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        // nothing to do
    }

    @Override
    public void postProcessEngineBuild(ProcessEngine processEngine) {
        // nothing to do
    }
}
