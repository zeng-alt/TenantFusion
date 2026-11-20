package com.github.zeng.alt.security.abac.client.cache;

import com.github.zeng.alt.security.abac.AbacContext;
import com.github.zeng.alt.security.abac.AbacFailure;
import com.github.zeng.alt.security.abac.AbacPolicy;
import com.github.zeng.alt.security.abac.client.config.AbacClientProperties;
import com.github.zeng.alt.security.abac.core.AbacEvaluator;
import com.github.zeng.alt.security.abac.core.AbacPolicyLoader;
import com.github.zeng.alt.security.abac.policy.AbacPolicyDto;
import com.github.zeng.alt.security.abac.policy.AbacPolicyQuery;
import com.github.zeng.alt.security.abac.policy.AbacPolicyQueryService;
import com.github.zeng.alt.security.abac.policy.AbacPolicySnapshotDto;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.base.Ticker;
import io.vavr.control.Either;
import io.vavr.control.Try;

import java.util.List;

/**
 * 按租户、权限合并首次加载，预解析成功后缓存快照；失败结果立即移出缓存。
 * 不缓存用户属性或授权结论，快照过期后重新查询权威来源。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public class CachedAbacPolicyLoader implements AbacPolicyLoader {
    private final AbacPolicyQueryService source;
    private final AbacEvaluator evaluator;
    private final Cache<AbacPolicyQuery, Either<AbacFailure, AbacPolicySnapshotDto>> cache;

    public CachedAbacPolicyLoader(
            AbacPolicyQueryService source, AbacEvaluator evaluator, AbacClientProperties properties) {
        this(source, evaluator, properties, Ticker.systemTicker());
    }

    CachedAbacPolicyLoader(
            AbacPolicyQueryService source, AbacEvaluator evaluator, AbacClientProperties properties, Ticker ticker) {
        this.source = source;
        this.evaluator = evaluator;
        this.cache = CacheBuilder.newBuilder().maximumSize(properties.getMaximumSize())
                .expireAfterWrite(properties.getCacheTtl()).ticker(ticker).build();
    }

    @Override
    public List<AbacPolicy> list(AbacContext context) {
        return get(new AbacPolicyQuery(context.tenant(), context.permission()))
                .map(snapshot -> snapshot.policies().stream().map(AbacPolicyDto::toPolicy).toList())
                .getOrElseThrow(failure -> new IllegalStateException(failure.code() + ": " + failure.message()));
    }

    public Either<AbacFailure, AbacPolicySnapshotDto> get(AbacPolicyQuery query) {
        final Either<AbacFailure, AbacPolicySnapshotDto> loaded = Try.of(() -> cache.get(query,
                        () -> source.get(query).flatMap(snapshot -> prepare(query, snapshot))))
                .toEither(new AbacFailure("POLICY_LOAD_FAILED", "策略加载或预解析失败"))
                .flatMap(value -> value);
        if (loaded.isLeft()) {
            cache.invalidate(query);
        }
        return loaded;
    }

    /** 先失效所有受影响键，刷新失败的键保持未缓存状态，后续授权不会使用旧许可。 */
    public Either<AbacFailure, Boolean> refresh(List<AbacPolicyQuery> keys) {
        cache.invalidateAll(keys);
        return io.vavr.collection.List.ofAll(keys).foldLeft(Either.<AbacFailure, Boolean>right(true),
                (previous, key) -> previous.flatMap(ignored -> get(key).map(snapshot -> true)));
    }

    private Either<AbacFailure, AbacPolicySnapshotDto> prepare(
            AbacPolicyQuery query, AbacPolicySnapshotDto snapshot) {
        if (!query.tenant().equals(snapshot.tenant()) || !query.permission().equals(snapshot.permission())
                || snapshot.policies().stream().anyMatch(policy -> !query.tenant().equals(policy.tenant())
                        || !query.permission().equals(policy.permission()))) {
            return Either.left(new AbacFailure("POLICY_SCOPE_MISMATCH", "策略快照不属于请求的租户或权限"));
        }
        return io.vavr.collection.List.ofAll(snapshot.policies()).foldLeft(
                Either.<AbacFailure, AbacPolicySnapshotDto>right(snapshot),
                (previous, policy) -> previous.flatMap(ready -> evaluator.prepare(policy.condition()).map(ignored -> ready)));
    }
}
