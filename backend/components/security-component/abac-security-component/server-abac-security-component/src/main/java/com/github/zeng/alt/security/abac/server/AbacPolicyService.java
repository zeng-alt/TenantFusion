package com.github.zeng.alt.security.abac.server;

import com.github.zeng.alt.security.abac.AbacFailure;
import com.github.zeng.alt.security.abac.AbacPolicy;
import io.vavr.control.Either;

/**
 * 规则管理入口，负责表达式预校验及成功写入后的缓存失效通知。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public interface AbacPolicyService {
    Either<AbacFailure, AbacPolicy> create(AbacPolicyCreateCmd cmd);

    Either<AbacFailure, AbacPolicy> update(AbacPolicy policy);

    Either<AbacFailure, Boolean> delete(String tenant, String id);
}
