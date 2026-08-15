package com.github.zeng.alt.camunda.identity.api;

import org.camunda.bpm.engine.impl.identity.ReadOnlyIdentityProvider;
import org.camunda.bpm.engine.impl.interceptor.Session;
import org.camunda.bpm.engine.impl.interceptor.SessionFactory;

/**
 * 为 Camunda 引擎提供自定义 {@link ReadOnlyIdentityProvider} 会话。
 * <p>
 * 由于底层数据来源（SPI 实现）无会话状态，每次直接返回同一 provider 实例。
 */
public class CamundaIdentityProviderFactory implements SessionFactory {

    private final CamundaIdentityProvider provider;

    public CamundaIdentityProviderFactory(CamundaIdentityProvider provider) {
        this.provider = provider;
    }

    @Override
    public Class<?> getSessionType() {
        return ReadOnlyIdentityProvider.class;
    }

    @Override
    public Session openSession() {
        return provider;
    }
}
