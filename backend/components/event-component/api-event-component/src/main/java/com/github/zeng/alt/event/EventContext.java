package com.github.zeng.alt.event;

import io.vavr.control.Option;

import java.util.Map;

/**
 * 只读的来源上下文；原生对象仅在当前监听调用内有效，不参与持久化。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public interface EventContext {
    String messageId();

    String listenerId();

    /** 组件已安排的重试次数，首次投递为 0；不包含 Broker 自身的重复投递。 */
    int retryCount();

    /** 返回随事件持久化的来源上下文快照。 */
    Map<String, String> attributes();

    Map<String, String> headers();

    default Option<String> attribute(String name) {
        return Option.of(attributes().get(name));
    }

    /** 获取当前传输的原生对象；对象只在本次监听调用内有效，未提供时返回 None。 */
    <T> Option<T> nativeContext(Class<T> type);
}
