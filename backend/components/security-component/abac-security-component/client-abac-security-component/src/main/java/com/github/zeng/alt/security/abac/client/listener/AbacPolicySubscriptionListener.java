package com.github.zeng.alt.security.abac.client.listener;

import com.github.zeng.alt.event.EventDelivery;
import com.github.zeng.alt.event.EventOutcome;
import com.github.zeng.alt.event.annotation.EventSubscription;
import com.github.zeng.alt.security.abac.client.cache.CachedAbacPolicyLoader;
import com.github.zeng.alt.security.abac.event.AbacPolicyChangedEvent;
import lombok.RequiredArgsConstructor;

/**
 * 远程变更订阅，刷新失败进入既有事件重试预算。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class AbacPolicySubscriptionListener {
    private final CachedAbacPolicyLoader loader;

    @EventSubscription(id = "abac-policy-client", binding = "${security.abac.client.notification-binding:abac-policies}")
    public EventOutcome refresh(AbacPolicyChangedEvent event, EventDelivery delivery) {
        return loader.refresh(event.keys()).fold(failure -> delivery.retry(), refreshed -> delivery.ack());
    }
}
