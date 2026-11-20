package com.github.zeng.alt.security.abac.server.spi;

import com.github.zeng.alt.security.abac.AbacFailure;
import com.github.zeng.alt.security.abac.AbacPolicy;
import com.github.zeng.alt.security.abac.server.AbacPolicyCreateCmd;
import io.vavr.control.Either;
import io.vavr.control.Option;

/**
 * 策略写入契约，由业务层选用 JPA、JDBC 或其他存储实现，组件不提供表映射。
 * 授权查询走权威读取 SPI。预期内失败返回 left，约定码包括
 * TENANT_MISMATCH、PERMISSION_NOT_FOUND、POLICY_NOT_FOUND；
 * 意外基础设施异常直接抛出，由调用方决定兜底方式。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public interface AbacPolicyStore {

    /** 在指定租户内读取旧策略，用于修改或删除后的精确失效通知。 */
    Option<AbacPolicy> findById(String tenant, String id);

    /**
     * 新建策略，返回携带存储生成 id 的策略快照。
     */
    Either<AbacFailure, AbacPolicy> create(AbacPolicyCreateCmd cmd);

    /**
     * 按 id 替换条件、授权阶段、资源范围与所属权限；策略租户不可变更。
     */
    Either<AbacFailure, AbacPolicy> update(AbacPolicy policy);

    /**
     * 按租户与 id 删除，成功返回 true；软删除或硬删除由实现决定。
     */
    Either<AbacFailure, Boolean> delete(String tenant, String id);
}
