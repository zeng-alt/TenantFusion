package com.github.zeng.alt.config.client;

import com.github.zeng.alt.config.ConfigSubscriptionHandler;
import com.github.zeng.alt.config.event.ConfigChangeEvent;
import com.github.zeng.alt.config.event.ConfigChangeListener;
import com.github.zeng.alt.config.model.ConfigChangedKey;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;

/**
 * 按键分发配置变更，同时支持 Spring listener bean。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Slf4j
@RequiredArgsConstructor
public class ConfigListenerTemplate {
    private final ConfigClientProperties properties;
    private final ObjectProvider<ConfigChangeListener> listeners;
    private final Map<ConfigChangedKey, List<ConfigChangeListener>> listenersByKey = new ConcurrentHashMap<>();

    public ConfigSubscriptionHandler subscribe(String key, String group, ConfigChangeListener listener) {
        final List<ConfigChangeListener> registrations = listenersByKey.computeIfAbsent(
            new ConfigChangedKey(key, group),
            ignored -> new CopyOnWriteArrayList<>()
        );
        registrations.add(listener);
        return () -> registrations.remove(listener);
    }

    @EventListener
    public void onConfigChange(ConfigChangeEvent event) {
        if (!properties.getAppCode().equals(event.getAppCode())) {
            return;
        }
        Stream.concat(
            listeners.orderedStream(),
            event.getChangedKeys().stream()
                    .flatMap(key -> Option.of(listenersByKey.get(key)).toJavaStream())
                    .flatMap(Collection::stream)
        )
                .distinct().forEach(listener -> notifyListener(listener, event));
    }

    private void notifyListener(ConfigChangeListener listener, ConfigChangeEvent event) {
        Try.run(() -> listener.onChange(event))
                .onFailure(error -> log.warn(
                    "Config listener failed for app={} ({})",
                    event.getAppCode(),
                    error.getClass().getSimpleName()
                ));
    }
}
