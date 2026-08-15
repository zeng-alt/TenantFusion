package com.github.zeng.alt.camunda.identity.api;

import org.camunda.bpm.engine.identity.Tenant;
import org.camunda.bpm.engine.impl.Page;
import org.camunda.bpm.engine.impl.TenantQueryImpl;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.interceptor.CommandExecutor;

import java.util.List;

/**
 * 自定义租户查询实现，由 {@link CamundaIdentityProvider} 执行实际过滤。
 */
public class CamundaTenantQuery extends TenantQueryImpl {

    private static final long serialVersionUID = 1L;

    private final CamundaIdentityProvider provider;

    public CamundaTenantQuery(CommandExecutor commandExecutor, CamundaIdentityProvider provider) {
        super(commandExecutor);
        this.provider = provider;
    }

    public CamundaTenantQuery(CamundaIdentityProvider provider) {
        super();
        this.provider = provider;
    }

    @Override
    public long executeCount(CommandContext commandContext) {
        return provider.findTenantCountByQueryCriteria(this);
    }

    @Override
    public List<Tenant> executeList(CommandContext commandContext, Page page) {
        return provider.executeTenantQuery(this);
    }
}
