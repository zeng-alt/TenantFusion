package com.github.zeng.alt.camunda.identity.api;

import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.impl.GroupQueryImpl;
import org.camunda.bpm.engine.impl.Page;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.interceptor.CommandExecutor;

import java.util.List;

/**
 * 自定义组查询实现，由 {@link CamundaIdentityProvider} 执行实际过滤。
 */
public class CamundaGroupQuery extends GroupQueryImpl {

    private static final long serialVersionUID = 1L;

    private final CamundaIdentityProvider provider;

    public CamundaGroupQuery(CommandExecutor commandExecutor, CamundaIdentityProvider provider) {
        super(commandExecutor);
        this.provider = provider;
    }

    public CamundaGroupQuery(CamundaIdentityProvider provider) {
        super();
        this.provider = provider;
    }

    @Override
    public long executeCount(CommandContext commandContext) {
        return provider.findGroupCountByQueryCriteria(this);
    }

    @Override
    public List<Group> executeList(CommandContext commandContext, Page page) {
        return provider.executeGroupQuery(this);
    }
}
