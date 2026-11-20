package com.github.zeng.alt.security.abac.client.listener;

import com.github.zeng.alt.event.EventDelivery;
import com.github.zeng.alt.security.abac.AbacFailure;
import com.github.zeng.alt.security.abac.client.cache.CachedAbacPolicyLoader;
import com.github.zeng.alt.security.abac.client.config.AbacClientProperties;
import com.github.zeng.alt.security.abac.event.AbacPolicyChangedEvent;
import com.github.zeng.alt.security.abac.policy.AbacPolicyDto;
import com.github.zeng.alt.security.abac.policy.AbacPolicyQuery;
import com.github.zeng.alt.security.abac.policy.AbacPolicySnapshotDto;
import com.github.zeng.alt.security.abac.spel.SpelAbacEvaluator;
import io.vavr.control.Either;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.event.TransactionalEventListenerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 实际 Spring 事务事件派发与远程订阅 ACK/重试回归。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AbacPolicyChangeListenerTest {
    private final AbacPolicyQuery query = new AbacPolicyQuery("t001", "update");
    private final AbacPolicyChangedEvent event = new AbacPolicyChangedEvent(List.of(query));
    private final AtomicReference<Either<AbacFailure, AbacPolicySnapshotDto>> response =
            new AtomicReference<>(Either.right(snapshot("true")));
    private final AtomicInteger reads = new AtomicInteger();
    private final CachedAbacPolicyLoader loader = new CachedAbacPolicyLoader(key -> {
        reads.incrementAndGet();
        return response.get();
    }, new SpelAbacEvaluator(), new AbacClientProperties());

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void commitRefreshesAfterCommitAndRollbackDoesNotRefresh() {
        try (final var context = context()) {
            assertThat(loader.get(query).get().policies().getFirst().condition()).isEqualTo("true");
            begin();
            response.set(Either.right(snapshot("false")));
            context.publishEvent(event);
            assertThat(reads).hasValue(1);
            TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            clearSynchronization();
            assertThat(loader.get(query).get().policies().getFirst().condition()).isEqualTo("true");

            begin();
            context.publishEvent(event);
            assertThat(reads).hasValue(1);
            TransactionSynchronizationUtils.triggerAfterCommit();
            TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_COMMITTED);
            clearSynchronization();
            assertThat(loader.get(query).get().policies().getFirst().condition()).isEqualTo("false");
            assertThat(reads).hasValue(2);
        }
    }

    @Test
    void localFailureLeavesTheCacheInvalidWithoutThrowingAfterCommit() {
        loader.get(query);
        response.set(Either.left(new AbacFailure("OFFLINE", "offline")));
        new AbacPolicyChangeListener(loader).refresh(event);
        assertThat(loader.get(query).isLeft()).isTrue();
        assertThat(reads).hasValue(3);
    }

    @Test
    void remoteRefreshAcknowledgesSuccessAndRetriesFailure() {
        final var listener = new AbacPolicySubscriptionListener(loader);
        final EventDelivery delivery = mock(EventDelivery.class);
        listener.refresh(event, delivery);
        verify(delivery).ack();
        verify(delivery, never()).retry();
        response.set(Either.left(new AbacFailure("OFFLINE", "offline")));
        listener.refresh(event, delivery);
        verify(delivery).retry();
        assertThat(loader.get(query).isLeft()).isTrue();
    }

    private AnnotationConfigApplicationContext context() {
        final var context = new AnnotationConfigApplicationContext();
        context.registerBean(TransactionalEventListenerFactory.class);
        context.registerBean(AbacPolicyChangeListener.class, () -> new AbacPolicyChangeListener(loader));
        context.refresh();
        return context;
    }

    private void begin() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }

    private AbacPolicySnapshotDto snapshot(String condition) {
        return new AbacPolicySnapshotDto("t001", "update", condition,
                List.of(new AbacPolicyDto("1", "t001", "update", condition, true)));
    }
}
