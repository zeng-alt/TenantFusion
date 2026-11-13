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
    int retryCount();
    Map<String, String> attributes();
    Map<String, String> headers();
    default Option<String> attribute(String name) {
        return Option.of(attributes().get(name));
    }
    <T> Option<T> nativeContext(Class<T> type);
}
