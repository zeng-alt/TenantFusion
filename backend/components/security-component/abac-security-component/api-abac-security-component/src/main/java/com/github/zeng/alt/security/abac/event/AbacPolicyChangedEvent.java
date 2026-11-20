package com.github.zeng.alt.security.abac.event;

import com.github.zeng.alt.security.abac.policy.AbacPolicyQuery;

import java.util.List;

/**
 * 规则变更通知。接收方按受影响键回源最新快照，不应用通知携带的旧策略内容。
 *
 * @param keys 需要失效与刷新的租户、权限组合
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record AbacPolicyChangedEvent(
    List<AbacPolicyQuery> keys
) {
    public AbacPolicyChangedEvent {
        keys = List.copyOf(keys).stream().distinct().toList();
    }
}
