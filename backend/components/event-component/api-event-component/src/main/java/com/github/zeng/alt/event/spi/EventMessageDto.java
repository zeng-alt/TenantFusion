package com.github.zeng.alt.event.spi;

import java.util.Objects;

/**
 * 传输内部使用的信封，不要求业务事件继承或增加字段。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record EventMessageDto(
        EventMetadataDto metadata,
        byte[] payload
) {
    public EventMessageDto(EventMetadataDto metadata, byte[] payload) {
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.payload = payload.clone();
    }

    @Override
    public byte[] payload() {
        return payload.clone();
    }

    public EventMessageDto withMetadata(EventMetadataDto metadata) {
        return new EventMessageDto(metadata, payload);
    }
}
