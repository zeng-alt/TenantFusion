package com.github.zeng.alt.security.abac.policy;

import com.github.zeng.alt.security.abac.AbacFailure;
import io.vavr.control.Either;

/**
 * 客户端统一的权威策略来源，本地服务和 HTTP 适配器实现相同契约。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@FunctionalInterface
public interface AbacPolicyQueryService {
    String PATH = "/internal/abac/policies";
    String CLIENT_HEADER = "X-Abac-Client";
    String KEY_HEADER = "X-Abac-Service-Key";
    String TENANT_HEADER = "X-Tenant-Id";

    Either<AbacFailure, AbacPolicySnapshotDto> get(AbacPolicyQuery query);
}
