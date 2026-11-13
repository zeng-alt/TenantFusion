package com.github.zeng.alt.event.internal;

import com.github.zeng.alt.event.context.EventContextTemplate;
import com.google.common.collect.MapMaker;
import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import org.springframework.context.PayloadApplicationEvent;

import java.util.Map;

/**
 * 在原始事件与持久化快照之间建立内部关联，不改变业务事件的类型或字段。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class EventContextHelper {

    // 弱引用按对象身份比较，避免两个租户的相等 record 共用上下文，也不延长事件生命周期。
    private final Map<Object, Map<String, String>> contextByEvent = new MapMaker().weakKeys().makeMap();
    private final EventContextTemplate contexts;

    public Map<String, String> capture(Object event) {
        return contextByEvent.computeIfAbsent(payload(event), ignored -> contexts.capture());
    }

    public void remember(Object event, Map<String, String> contextByKey) {
        contextByEvent.put(payload(event), Map.copyOf(contextByKey));
    }

    public Map<String, String> getContext(Object event) {
        return Option.of(contextByEvent.get(payload(event))).getOrElse(contexts::capture);
    }

    private static Object payload(Object event) {
        return event instanceof PayloadApplicationEvent<?> wrapper ? wrapper.getPayload() : event;
    }
}
