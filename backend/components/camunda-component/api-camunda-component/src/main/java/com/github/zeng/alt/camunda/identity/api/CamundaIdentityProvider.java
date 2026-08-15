package com.github.zeng.alt.camunda.identity.api;

import org.camunda.bpm.engine.BadUserRequestException;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.GroupQuery;
import org.camunda.bpm.engine.identity.NativeUserQuery;
import org.camunda.bpm.engine.identity.Tenant;
import org.camunda.bpm.engine.identity.TenantQuery;
import org.camunda.bpm.engine.identity.User;
import org.camunda.bpm.engine.identity.UserQuery;
import org.camunda.bpm.engine.impl.Page;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.identity.ReadOnlyIdentityProvider;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.interceptor.Session;
import org.camunda.bpm.engine.impl.persistence.entity.GroupEntity;
import org.camunda.bpm.engine.impl.persistence.entity.TenantEntity;
import org.camunda.bpm.engine.impl.persistence.entity.UserEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 基于 {@link CamundaUserGroupSource} 的只读 IdentityProvider。
 * <p>
 * 将 admin 用户/角色体系桥接到 Camunda 引擎，用于 webapp 登录、任务候选人查询等。
 */
public class CamundaIdentityProvider implements ReadOnlyIdentityProvider, Session {

    private final CamundaUserGroupSource source;
    private final CamundaTenantSource tenantSource;

    public CamundaIdentityProvider(CamundaUserGroupSource source) {
        this(source, null);
    }

    public CamundaIdentityProvider(CamundaUserGroupSource source, CamundaTenantSource tenantSource) {
        this.source = source;
        this.tenantSource = tenantSource;
    }

    // ----- Session -----

    @Override
    public void flush() {
        // nothing to do
    }

    @Override
    public void close() {
        // nothing to do
    }

    // ----- Users -----

    @Override
    public User findUserById(String userId) {
        return source.findByUsername(userId)
                .map(this::toUserEntity)
                .orElse(null);
    }

    @Override
    public UserQuery createUserQuery() {
        return new CamundaUserQuery(Context.getProcessEngineConfiguration().getCommandExecutorTxRequired(), this);
    }

    @Override
    public UserQuery createUserQuery(CommandContext commandContext) {
        return new CamundaUserQuery(this);
    }

    @Override
    public NativeUserQuery createNativeUserQuery() {
        throw new BadUserRequestException("Native user queries are not supported by the custom identity provider.");
    }

    @Override
    public boolean checkPassword(String userId, String password) {
        if (userId == null || userId.isBlank() || password == null) {
            return false;
        }
        return source.matchesPassword(userId, password);
    }

    // ----- Groups -----

    @Override
    public Group findGroupById(String groupId) {
        return source.findByGroupCode(groupId)
                .map(this::toGroupEntity)
                .orElse(null);
    }

    @Override
    public GroupQuery createGroupQuery() {
        return new CamundaGroupQuery(Context.getProcessEngineConfiguration().getCommandExecutorTxRequired(), this);
    }

    @Override
    public GroupQuery createGroupQuery(CommandContext commandContext) {
        return new CamundaGroupQuery(this);
    }

    // ----- Tenants -----

    @Override
    public Tenant findTenantById(String tenantId) {
        if (tenantSource == null) {
            return null;
        }
        return tenantSource.findTenantById(tenantId)
                .map(this::toTenantEntity)
                .orElse(null);
    }

    @Override
    public TenantQuery createTenantQuery() {
        return new CamundaTenantQuery(Context.getProcessEngineConfiguration().getCommandExecutorTxRequired(), this);
    }

    @Override
    public TenantQuery createTenantQuery(CommandContext commandContext) {
        return new CamundaTenantQuery(this);
    }

    // ----- Internal query execution (called by query implementations) -----

    public long findTenantCountByQueryCriteria(CamundaTenantQuery query) {
        return executeTenantListQuery(query).size();
    }

    @SuppressWarnings("unchecked")
    public List<Tenant> executeTenantQuery(CamundaTenantQuery query) {
        return executeTenantListQuery(query).stream()
                .map(this::toTenantEntity)
                .map(t -> (Tenant) t)
                .toList();
    }

    public long findUserCountByQueryCriteria(CamundaUserQuery query) {
        return executeUserQuery(query).size();
    }

    @SuppressWarnings("unchecked")
    public List<User> findUserByQueryCriteria(CamundaUserQuery query) {
        return executeUserQuery(query).stream()
                .map(this::toUserEntity)
                .map(u -> (User) u)
                .toList();
    }

    public long findGroupCountByQueryCriteria(CamundaGroupQuery query) {
        return executeGroupQuery(query).size();
    }

    @SuppressWarnings("unchecked")
    public List<Group> executeGroupQuery(CamundaGroupQuery query) {
        return executeGroupListQuery(query).stream()
                .map(this::toGroupEntity)
                .map(g -> (Group) g)
                .toList();
    }

    private List<CamundaIdentityUser> executeUserQuery(CamundaUserQuery query) {
        List<CamundaIdentityUser> users;
        if (query.getId() != null) {
            users = source.findByUsername(query.getId())
                    .map(List::of)
                    .orElseGet(List::of);
        } else if (query.getIds() != null && query.getIds().length > 0) {
            users = java.util.Arrays.stream(query.getIds())
                    .map(source::findByUsername)
                    .flatMap(opt -> opt.stream())
                    .toList();
        } else if (query.getGroupId() != null) {
            users = source.findUsersByGroupCode(query.getGroupId());
        } else {
            // 只读 provider 不暴露全量扫描（与 LDAP 行为一致）
            users = Collections.emptyList();
        }
        return applyPage(users, query.getFirstResult(), query.getMaxResults());
    }

    private List<CamundaIdentityGroup> executeGroupListQuery(CamundaGroupQuery query) {
        List<CamundaIdentityGroup> groups;
        if (query.getUserId() != null) {
            groups = source.findGroupsByUsername(query.getUserId());
        } else if (query.getId() != null) {
            groups = source.findByGroupCode(query.getId())
                    .map(List::of)
                    .orElseGet(List::of);
        } else if (query.getIds() != null && query.getIds().length > 0) {
            groups = java.util.Arrays.stream(query.getIds())
                    .map(source::findByGroupCode)
                    .flatMap(opt -> opt.stream())
                    .toList();
        } else {
            groups = source.findAllGroups();
        }
        return applyPage(groups, query.getFirstResult(), query.getMaxResults());
    }

    private List<CamundaIdentityTenant> executeTenantListQuery(CamundaTenantQuery query) {
        if (tenantSource == null) {
            return Collections.emptyList();
        }
        List<CamundaIdentityTenant> tenants;
        if (query.getUserId() != null) {
            tenants = tenantSource.findTenantsByUsername(query.getUserId());
        } else if (query.getId() != null) {
            tenants = tenantSource.findTenantById(query.getId())
                    .map(List::of)
                    .orElseGet(List::of);
        } else if (query.getIds() != null && query.getIds().length > 0) {
            tenants = Arrays.stream(query.getIds())
                    .map(tenantSource::findTenantById)
                    .flatMap(opt -> opt.stream())
                    .toList();
        } else {
            tenants = tenantSource.findAllTenants();
        }
        String name = query.getName();
        if (name != null && !name.isBlank()) {
            tenants = tenants.stream()
                    .filter(t -> name.equals(t.name()))
                    .toList();
        }
        String nameLike = query.getNameLike();
        if (nameLike != null && !nameLike.isBlank()) {
            tenants = tenants.stream()
                    .filter(t -> t.name() != null && t.name().toLowerCase(Locale.ROOT).contains(nameLike.toLowerCase(Locale.ROOT)))
                    .toList();
        }
        return applyPage(tenants, query.getFirstResult(), query.getMaxResults());
    }

    private <T> List<T> applyPage(List<T> data, int firstResult, int maxResults) {
        int end = Math.min(data.size(), firstResult + maxResults);
        if (firstResult >= data.size() || firstResult < 0 || end < 0) {
            return Collections.emptyList();
        }
        return data.subList(firstResult, end);
    }

    private UserEntity toUserEntity(CamundaIdentityUser user) {
        UserEntity entity = new UserEntity();
        entity.setId(user.id());
        entity.setFirstName(user.firstName());
        entity.setLastName(user.lastName());
        entity.setEmail(user.email());
        return entity;
    }

    private GroupEntity toGroupEntity(CamundaIdentityGroup group) {
        GroupEntity entity = new GroupEntity();
        entity.setId(group.id());
        entity.setName(group.name());
        entity.setType(group.type());
        return entity;
    }

    private TenantEntity toTenantEntity(CamundaIdentityTenant tenant) {
        TenantEntity entity = new TenantEntity();
        entity.setId(tenant.id());
        entity.setName(tenant.name());
        return entity;
    }
}
