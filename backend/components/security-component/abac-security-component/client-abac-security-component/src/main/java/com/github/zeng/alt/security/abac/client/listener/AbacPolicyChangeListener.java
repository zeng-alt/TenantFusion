package com.github.zeng.alt.security.abac.client.listener;

import com.github.zeng.alt.security.abac.client.cache.CachedAbacPolicyLoader;
import com.github.zeng.alt.security.abac.event.AbacPolicyChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 同进程规则提交后刷新，不在回滚时改动执行缓存。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Slf4j
@RequiredArgsConstructor
public class AbacPolicyChangeListener {
    private final CachedAbacPolicyLoader loader;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void refresh(AbacPolicyChangedEvent event) {
        loader.refresh(event.keys()).peekLeft(failure ->
                log.warn("ABAC local refresh failed: keys={} code={}", event.keys(), failure.code()));
    }
}
