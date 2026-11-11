package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.admin.infrastructure.entity.PolicyRule;
import com.github.zeng.alt.security.abac.AbacPolicy;
import com.github.zeng.alt.security.abac.core.AbacPolicyCreateCmd;
import io.vavr.control.Option;
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
@Mapper(
        componentModel = "spring",
        imports = Option.class
)
public interface AbacPolicyMapper {

    /**
     * 未声明预授权的规则映射为非预授权，由组件明确拒绝不支持的授权阶段。
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
    @Mapping(
            target = "resourceId",
            expression = "java(Option.of(rule.getResourceId()).map(String::valueOf))"
    )
    AbacPolicy toPolicy(PolicyRule rule);

    /**
     * 新建规则骨架：主键由存储生成，租户列由调用方显式赋当前租户（与 {@code @TenantId} 插入回填一致），
     * 权限关联与数值化资源 ID 由调用方查库、转换后设置。
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
    @Mapping(
            target = "resourceId",
            ignore = true
    )
    PolicyRule toEntity(AbacPolicyCreateCmd cmd);

    /**
     * 仅替换条件与授权阶段；身份、租户、权限关联和资源范围由调用方设置。
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
    @Mapping(
            target = "resourceId",
            ignore = true
    )
    void merge(AbacPolicy policy, @MappingTarget PolicyRule rule);
}
