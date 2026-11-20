package com.github.zeng.alt.security.abac.server.event;

import com.github.zeng.alt.event.EventTemplate;
import com.github.zeng.alt.security.abac.event.AbacPolicyChangedEvent;
import com.github.zeng.alt.security.abac.server.config.AbacServerProperties;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 仅在事务成功提交后广播；发送失败保留数据库提交，客户端由 TTL 回源补偿。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Slf4j
@RequiredArgsConstructor
public class AbacPolicyEventListener {
    private final EventTemplate events;
    private final AbacServerProperties properties;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void publish(AbacPolicyChangedEvent event) {
        Try.of(() -> events.to(properties.getNotificationBinding()).publish(event))
                .onFailure(failure -> log.warn("ABAC change notification failed for keys={}", event.keys(), failure));
    }
}
