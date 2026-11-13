package com.github.zeng.alt.event.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.event.EventException;
import com.github.zeng.alt.event.spi.EventMessageDto;
import com.github.zeng.alt.event.spi.EventMetadataDto;

import io.vavr.control.Option;
import io.vavr.control.Try;

import lombok.RequiredArgsConstructor;

import java.lang.reflect.Type;

/**
 * 消息编解码；按监听器声明的泛型类型读取，不从网络内容加载 Java 类。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class EventCodec {
    public static final String METADATA_HEADER = "alt-event-metadata";
    private final ObjectMapper mapper;

    public byte[] encode(Object value) {
        return Try.of(() -> mapper.writeValueAsBytes(value))
                .getOrElseThrow(cause -> new EventException("Cannot encode event", cause));
    }

    public <T> T decode(byte[] bytes, Class<T> type) {
        return Try.of(() -> mapper.readValue(bytes, type)).mapTry(
                value -> Option.of(value).getOrElseThrow(() -> new EventException("Event JSON must not be null"))
        ).getOrElseThrow(cause -> new EventException("Cannot decode event", cause));
    }

    public Object payload(EventMessageDto message, Type type) {
        return Try.of(() -> mapper.readValue(message.payload(), mapper.constructType(type))).mapTry(
                value -> Option.of(value).getOrElseThrow(() -> new EventException("Listener payload must not be null"))
        ).getOrElseThrow(cause -> new EventException("Cannot decode listener payload", cause));
    }

    public EventMetadataDto metadata(byte[] bytes) {
        return decode(bytes, EventMetadataDto.class);
    }
}
