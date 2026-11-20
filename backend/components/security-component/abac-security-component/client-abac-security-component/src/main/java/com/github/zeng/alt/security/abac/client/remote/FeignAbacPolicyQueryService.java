package com.github.zeng.alt.security.abac.client.remote;

import com.github.zeng.alt.security.abac.AbacFailure;
import com.github.zeng.alt.security.abac.policy.AbacPolicyQuery;
import com.github.zeng.alt.security.abac.policy.AbacPolicyQueryService;
import com.github.zeng.alt.security.abac.policy.AbacPolicySnapshotDto;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;

/**
 * Feign 集群查询适配，查询租户同时用于协议参数与服务身份请求头。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class FeignAbacPolicyQueryService implements AbacPolicyQueryService {
    private final AbacPolicyFeignClient client;

    @Override
    public Either<AbacFailure, AbacPolicySnapshotDto> get(AbacPolicyQuery query) {
        return Try.of(() -> client.get(query.tenant(), query.permission(), query.tenant()))
                .toEither(new AbacFailure("POLICY_REMOTE_FAILED", "远程策略查询失败"))
                .flatMap(snapshot -> Option.of(snapshot)
                        .toEither(new AbacFailure("POLICY_REMOTE_EMPTY", "远程策略响应为空")));
    }
}
