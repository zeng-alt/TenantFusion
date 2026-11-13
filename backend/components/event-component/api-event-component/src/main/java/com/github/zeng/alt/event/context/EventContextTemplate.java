package com.github.zeng.alt.event.context;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.task.TaskDecorator;

import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 按 Bean 顺序组合上下文贡献者，也可直接作为 Spring 线程池的 TaskDecorator。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class EventContextTemplate implements TaskDecorator {

    private final ObjectProvider<EventContextHandler> handlers;

    public Map<String, String> capture() {
        return handlers.orderedStream().flatMap(handler -> handler.capture().entrySet().stream()
                        .map(entry -> Map.entry(handler.key() + "." + entry.getKey(), entry.getValue())))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /** 按正序恢复、逆序还原，保证嵌套调用及异常退出后不残留本次上下文。 */
    public EventContextHandler.Scope restore(Map<String, String> contextByKey) {
        final var scopes = new ArrayList<EventContextHandler.Scope>();
        try {
            handlers.orderedStream().forEach(handler -> {
                final String prefix = handler.key() + ".";
                final Map<String, String> localContextByKey = contextByKey.entrySet().stream()
                        .filter(entry -> entry.getKey().startsWith(prefix))
                        .collect(Collectors.toUnmodifiableMap(
                                entry -> entry.getKey().substring(prefix.length()),
                                Map.Entry::getValue
                        ));
                scopes.add(handler.restore(localContextByKey));
            });
        } catch (RuntimeException | Error failure) {
            scopes.reversed().forEach(EventContextHandler.Scope::close);
            throw failure;
        }
        return () -> scopes.reversed().forEach(EventContextHandler.Scope::close);
    }

    @Override
    public Runnable decorate(Runnable runnable) {
        final Map<String, String> contextByKey = capture();
        return () -> {
            try (final var scope = restore(contextByKey)) {
                runnable.run();
            }
        };
    }
}
