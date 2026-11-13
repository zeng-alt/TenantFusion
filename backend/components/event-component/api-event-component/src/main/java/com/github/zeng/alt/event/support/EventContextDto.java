package com.github.zeng.alt.event.support;

import com.github.zeng.alt.event.EventContext;
import com.github.zeng.alt.event.spi.EventMetadataDto;

import io.vavr.control.Option;

import java.util.List;
import java.util.Map;

/**
 * 当前一次调用的只读上下文视图。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record EventContextDto(
        String listenerId,
        EventMetadataDto metadata,
        List<Object> nativeContexts
) implements EventContext {
    public EventContextDto(String listenerId, EventMetadataDto metadata, List<Object> nativeContexts) {
        this.listenerId = listenerId;
        this.metadata = metadata;
        this.nativeContexts = List.copyOf(nativeContexts);
    }

    @Override
    public String messageId() {
        return metadata.id();
    }

    @Override
    public int retryCount() {
        return metadata.retry().count();
    }

    @Override
    public Map<String, String> attributes() {
        return metadata.context();
    }

    @Override
    public Map<String, String> headers() {
        return metadata.headers();
    }

    @Override
    public <T> Option<T> nativeContext(Class<T> type) {
        return Option.ofOptional(nativeContexts.stream().filter(type::isInstance).map(type::cast).findFirst());
    }
}
