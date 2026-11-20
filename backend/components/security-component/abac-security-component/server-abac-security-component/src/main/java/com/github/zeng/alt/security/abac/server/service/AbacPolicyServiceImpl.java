package com.github.zeng.alt.security.abac.server.service;

import com.github.zeng.alt.security.abac.AbacFailure;
import com.github.zeng.alt.security.abac.AbacPolicy;
import com.github.zeng.alt.security.abac.core.AbacEvaluator;
import com.github.zeng.alt.security.abac.event.AbacPolicyChangedEvent;
import com.github.zeng.alt.security.abac.policy.AbacPolicyQuery;
import com.github.zeng.alt.security.abac.server.AbacPolicyCreateCmd;
import com.github.zeng.alt.security.abac.server.AbacPolicyService;
import com.github.zeng.alt.security.abac.server.spi.AbacPolicyStore;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 管理写操作与变更发布，外层事务回滚时不触发客户端和消息订阅者。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class AbacPolicyServiceImpl implements AbacPolicyService {
    private final AbacPolicyStore store;
    private final AbacEvaluator evaluator;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Either<AbacFailure, AbacPolicy> create(AbacPolicyCreateCmd cmd) {
        return evaluator.prepare(cmd.condition()).flatMap(ignored -> store.create(cmd))
                .peek(policy -> events.publishEvent(new AbacPolicyChangedEvent(List.of(
                        new AbacPolicyQuery(policy.tenant(), policy.permission())))));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Either<AbacFailure, AbacPolicy> update(AbacPolicy policy) {
        return evaluator.prepare(policy.condition())
                .flatMap(ignored -> store.findById(policy.tenant(), policy.id())
                        .toEither(new AbacFailure("POLICY_NOT_FOUND", "策略不存在")))
                .flatMap(previous -> store.update(policy).peek(updated -> events.publishEvent(
                        new AbacPolicyChangedEvent(List.of(
                                new AbacPolicyQuery(previous.tenant(), previous.permission()),
                                new AbacPolicyQuery(updated.tenant(), updated.permission()))))));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Either<AbacFailure, Boolean> delete(String tenant, String id) {
        return store.findById(tenant, id).toEither(new AbacFailure("POLICY_NOT_FOUND", "策略不存在"))
                .flatMap(previous -> store.delete(tenant, id).peek(deleted -> {
                    if (deleted) {
                        events.publishEvent(new AbacPolicyChangedEvent(List.of(
                                new AbacPolicyQuery(previous.tenant(), previous.permission()))));
                    }
                }));
    }
}
