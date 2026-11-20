package com.github.zeng.alt.security.abac.server.service;

import com.github.zeng.alt.security.abac.AbacFailure;
import com.github.zeng.alt.security.abac.policy.AbacPolicyDto;
import com.github.zeng.alt.security.abac.policy.AbacPolicyQuery;
import com.github.zeng.alt.security.abac.policy.AbacPolicyQueryService;
import com.github.zeng.alt.security.abac.policy.AbacPolicySnapshotDto;
import com.github.zeng.alt.security.abac.server.spi.AbacPolicyRepository;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import io.vavr.control.Either;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * 权威策略查询与稳定内容指纹，同进程和 HTTP 端点共用此实现。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class AbacPolicyQueryServiceImpl implements AbacPolicyQueryService {
    private final AbacPolicyRepository repository;

    @Override
    public Either<AbacFailure, AbacPolicySnapshotDto> get(AbacPolicyQuery query) {
        return Try.of(() -> repository.list(query))
                .toEither(new AbacFailure("POLICY_QUERY_FAILED", "权威策略查询失败"))
                .flatMap(result -> result)
                .map(policies -> policies.stream().map(AbacPolicyDto::from).toList())
                .filterOrElse(policies -> policies.stream().allMatch(policy -> query.tenant().equals(policy.tenant())
                                && query.permission().equals(policy.permission())),
                        ignored -> new AbacFailure("POLICY_SCOPE_MISMATCH", "策略不属于请求的租户或权限"))
                .map(policies -> new AbacPolicySnapshotDto(
                        query.tenant(), query.permission(), getRevision(policies), policies));
    }

    private String getRevision(List<AbacPolicyDto> policies) {
        final Hasher hasher = Hashing.sha256().newHasher();
        policies.stream().sorted(Comparator.comparing(AbacPolicyDto::id))
                .forEach(policy -> Stream.of(policy.id(), policy.tenant(), policy.permission(), policy.condition(),
                        Boolean.toString(policy.preAuth()))
                .forEach(value -> hasher.putInt(value.length()).putUnencodedChars(value)));
        return hasher.hash().toString();
    }
}
