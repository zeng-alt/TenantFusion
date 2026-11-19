package com.github.zeng.alt.event.context;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ObjectMapper;
import com.github.zeng.alt.event.internal.ContextEventSerializer;
import com.github.zeng.alt.event.internal.EventContextHelper;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.modulith.events.core.EventSerializer;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证旧记录读取、跨实例快照恢复及相等 record 的上下文隔离。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class ContextEventSerializerTest {

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void reloadsContextWithoutOriginalInMemoryAssociation() {
        final var contexts = contexts();
        final var writer = new ContextEventSerializer(delegate(), new EventContextHelper(contexts));
        TenantContextHolder.setTenantId("original");
        final Object serialized = writer.serialize(new BusinessEvent("1"));
        TenantContextHolder.setTenantId("reader");
        final var reloadedSnapshots = new EventContextHelper(contexts);
        final var reader = new ContextEventSerializer(delegate(), reloadedSnapshots);
        final BusinessEvent event = reader.deserialize(serialized, BusinessEvent.class);
        assertThat(event).isEqualTo(new BusinessEvent("1"));
        assertThat(reloadedSnapshots.getContext(event)).containsEntry("request.tenantId", "original");
        assertThat(reader.serialize(event)).isEqualTo(serialized);
        assertThat(TenantContextHolder.getTenantId()).isEqualTo("reader");
        try (final var scope = contexts.restore(reloadedSnapshots.getContext(event))) {
            assertThat(TenantContextHolder.getTenantId()).isEqualTo("original");
        }
        assertThat(TenantContextHolder.getTenantId()).isEqualTo("reader");
    }

    @Test
    void readsLegacyPayloadWithoutUsingReadersTenantAsOriginalContext() {
        final var snapshots = new EventContextHelper(contexts());
        final var serializer = new ContextEventSerializer(delegate(), snapshots);
        TenantContextHolder.setTenantId("reader");
        final BusinessEvent event = serializer.deserialize("{\"id\":\"legacy\"}", BusinessEvent.class);
        assertThat(event.id()).isEqualTo("legacy");
        assertThat(snapshots.getContext(event)).isEmpty();
        assertThat(TenantContextHolder.getTenantId()).isEqualTo("reader");
    }

    @Test
    void keepsEqualRecordsFromDifferentTenantsSeparate() {
        final var snapshots = new EventContextHelper(contexts());
        final var first = new BusinessEvent("same-id");
        final var second = new BusinessEvent("same-id");
        snapshots.remember(first, Map.of("request.tenantId", "first"));
        snapshots.remember(second, Map.of("request.tenantId", "second"));
        assertThat(snapshots.getContext(first)).containsEntry("request.tenantId", "first");
        assertThat(snapshots.getContext(second)).containsEntry("request.tenantId", "second");
    }

    private static EventContextTemplate contexts() {
        final var factory = new DefaultListableBeanFactory();
        factory.registerSingleton("request", new RequestEventContextHandler());
        return new EventContextTemplate(factory.getBeanProvider(EventContextHandler.class));
    }

    private static EventSerializer delegate() {
        final var mapper = JsonMapper.builder().build();
        final var delegate = mock(EventSerializer.class);
        when(delegate.serialize(any())).thenAnswer(invocation -> mapper.writeValueAsString(invocation.getArgument(0)));
        when(delegate.deserialize(any(), eq(BusinessEvent.class))).thenAnswer(invocation ->
                mapper.readValue(invocation.getArgument(0).toString(), BusinessEvent.class));
        return delegate;
    }

    /**
     * 无框架接口、无上下文字段的业务事件。
     *
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    public record BusinessEvent(String id) {
    }
}
