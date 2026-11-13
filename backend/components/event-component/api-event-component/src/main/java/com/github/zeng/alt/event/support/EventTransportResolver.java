package com.github.zeng.alt.event.support;

import com.github.zeng.alt.event.EventException;
import com.github.zeng.alt.event.config.EventBindingProperties;
import com.github.zeng.alt.event.config.EventProperties;
import com.github.zeng.alt.event.spi.EventTransportTemplate;
import io.vavr.control.Option;
import org.springframework.beans.factory.ObjectProvider;
import lombok.RequiredArgsConstructor;
import java.util.List;

/**
 * 按绑定选择唯一传输实现，不使用 Primary 猜测中间件。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class EventTransportResolver {
    private final EventProperties properties;
    private final ObjectProvider<EventTransportTemplate> transports;
    public EventBindingProperties binding(String name) {
        final var binding = Option.of(properties.getBindings().get(name))
                .getOrElseThrow(() -> new EventException("Unknown event binding: " + name));
        binding.validate();
        return binding;
    }
    public EventTransportTemplate transport(String name) {
        final List<EventTransportTemplate> matches = transports.orderedStream()
                .filter(template -> template.transport().equals(name)).toList();
        if (matches.size() != 1) {
            throw new EventException("Expected one event transport for " + name + ", found " + matches.size());
        }
        return matches.getFirst();
    }
}
