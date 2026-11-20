package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.admin.infrastructure.entity.PolicyRule;
import com.github.zeng.alt.admin.infrastructure.entity.Permission;
import com.github.zeng.alt.admin.query.api.dto.AbacPermissionVO;
import com.github.zeng.alt.security.abac.AbacPolicy;
import com.github.zeng.alt.security.abac.server.AbacPolicyCreateCmd;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/**
 * 将持久化规则映射为 ABAC 组件使用的策略快照。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Mapper(componentModel = "spring")
public interface AbacPolicyMapper {
    @Mapping(target = "id", source = "permissionId")
    @Mapping(target = "name", source = "name", defaultValue = "")
    AbacPermissionVO toPermissionVO(Permission permission);


    /**
     * 未声明预授权的规则映射为后置授权，运行时要求活动事务。
     * 所属权限取自惰性关联，读取侧查询必须 join fetch，避免过滤器链中脱管后抛异常。
     */
    @Mapping(
            target = "id",
            source = "policyRuleId"
    )
    @Mapping(
            target = "tenant",
            source = "tenantBy"
    )
    @Mapping(
            target = "permission",
            source = "permission.code"
    )
    @Mapping(
            target = "preAuth",
            expression = "java(Boolean.TRUE.equals(rule.getPreAuth()))"
    )
    AbacPolicy toPolicy(PolicyRule rule);

    /**
     * 新建规则骨架：主键由存储生成，租户列由调用方显式赋当前租户（与 {@code @TenantId} 插入回填一致），
     * 权限关联由调用方查库后设置。
     */
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(
            target = "policyRuleId",
            ignore = true
    )
    @Mapping(
            target = "tenantBy",
            ignore = true
    )
    @Mapping(
            target = "permission",
            ignore = true
    )
    PolicyRule toEntity(AbacPolicyCreateCmd cmd);

    /**
     * 仅替换条件与授权阶段；身份、租户、权限关联由调用方设置。
     */
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(
            target = "policyRuleId",
            ignore = true
    )
    @Mapping(
            target = "tenantBy",
            ignore = true
    )
    @Mapping(
            target = "permission",
            ignore = true
    )
    void merge(AbacPolicy policy, @MappingTarget PolicyRule rule);
}
