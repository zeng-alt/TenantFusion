package com.github.zeng.alt.security.abac.server.event;

import com.github.zeng.alt.event.EventTemplate;
import com.github.zeng.alt.security.abac.event.AbacPolicyChangedEvent;
import com.github.zeng.alt.security.abac.policy.AbacPolicyQuery;
import com.github.zeng.alt.security.abac.server.config.AbacServerProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.event.TransactionalEventListenerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 服务端事务提交后才向既有事件通道广播，发送失败不反转已提交规则。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AbacPolicyEventListenerTest {
    private final EventTemplate events = mock(EventTemplate.class);
    private final EventTemplate.EventPublishBuilder publication = mock(EventTemplate.EventPublishBuilder.class);
    private final AbacServerProperties properties = new AbacServerProperties();
    private final AbacPolicyChangedEvent event = new AbacPolicyChangedEvent(
            List.of(new AbacPolicyQuery("t001", "update")));

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void publishesToTheConfiguredBindingOnlyAfterCommit() {
        properties.setNotificationBinding("rules");
        when(events.to("rules")).thenReturn(publication);
        when(publication.publish(event)).thenReturn("event-1");
        try (final var context = context()) {
            begin();
            context.publishEvent(event);
            verifyNoInteractions(events, publication);
            TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            clearSynchronization();
            verifyNoInteractions(events, publication);

            begin();
            context.publishEvent(event);
            verifyNoInteractions(events, publication);
            TransactionSynchronizationUtils.triggerAfterCommit();
            TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_COMMITTED);
            clearSynchronization();
            verify(events).to("rules");
            verify(publication).publish(event);
        }
    }

    @Test
    void publicationFailureDoesNotThrowAfterDatabaseCommit() {
        when(events.to("abac-policies")).thenThrow(new IllegalStateException("broker offline"));
        assertThatCode(() -> new AbacPolicyEventListener(events, properties).publish(event)).doesNotThrowAnyException();
    }

    private AnnotationConfigApplicationContext context() {
        final var context = new AnnotationConfigApplicationContext();
        context.registerBean(TransactionalEventListenerFactory.class);
        context.registerBean(AbacPolicyEventListener.class, () -> new AbacPolicyEventListener(events, properties));
        context.refresh();
        return context;
    }

    private void begin() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }
}
