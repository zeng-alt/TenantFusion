package com.github.zeng.alt.config.client;

import com.github.zeng.alt.config.event.ConfigChangeEvent;
import com.github.zeng.alt.config.event.ConfigDataChangedEvent;
import com.github.zeng.alt.config.model.ConfigChangedKey;
import com.github.zeng.alt.config.model.ConfigItemDTO;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@RequiredArgsConstructor
public class ConfigSubscriber {
    private final ConfigCacheManager cacheManager;
    private final ApplicationEventPublisher eventPublisher;
    private final ConfigClientProperties properties;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onConfigDataChanged(ConfigDataChangedEvent event) {
        if (properties.getAppCode().equals(event.getAppCode())) {
            acceptSnapshot(event.getItems());
        }
    }

    public synchronized void acceptSnapshot(List<ConfigItemDTO> items) {
        final Map<ConfigChangedKey, ConfigItemDTO> before = cacheManager.getSnapshot();
        cacheManager.replaceAll(items);
        final Map<ConfigChangedKey, ConfigItemDTO> after = cacheManager.getSnapshot();
        final List<ConfigChangedKey> changedKeys = Stream.concat(before.keySet().stream(), after.keySet().stream())
                .distinct().filter(key -> changed(before.get(key), after.get(key))).toList();
        if (!changedKeys.isEmpty()) {
            eventPublisher.publishEvent(new ConfigChangeEvent(this, properties.getAppCode(), changedKeys));
        }
    }

    private boolean changed(ConfigItemDTO before, ConfigItemDTO after) {
        if (before == null || after == null) {
            return before != after;
        }
        return !Objects.equals(before.getContent(), after.getContent())
                || !Objects.equals(before.getFormat(), after.getFormat());
    }
}
