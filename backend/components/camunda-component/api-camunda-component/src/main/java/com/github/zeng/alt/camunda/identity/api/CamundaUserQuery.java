package com.github.zeng.alt.camunda.identity.api;

import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.Page;
import org.operaton.bpm.engine.impl.UserQueryImpl;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;

import java.util.List;

/**
 * 自定义用户查询实现，由 {@link CamundaIdentityProvider} 执行实际过滤。
 */
public class CamundaUserQuery extends UserQueryImpl {

    private static final long serialVersionUID = 1L;

    private final CamundaIdentityProvider provider;

    public CamundaUserQuery(CommandExecutor commandExecutor, CamundaIdentityProvider provider) {
        super(commandExecutor);
        this.provider = provider;
    }

    public CamundaUserQuery(CamundaIdentityProvider provider) {
        super();
        this.provider = provider;
    }

    @Override
    public long executeCount(CommandContext commandContext) {
        return provider.findUserCountByQueryCriteria(this);
    }

    @Override
    public List<User> executeList(CommandContext commandContext, Page page) {
        return provider.findUserByQueryCriteria(this);
    }
}
