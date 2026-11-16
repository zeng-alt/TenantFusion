package com.github.zeng.alt.config.client;

import com.github.zeng.alt.config.event.ConfigChangeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.event.EventListener;

@Slf4j
@ConditionalOnClass(ContextRefresher.class)
public class ConfigRefreshBridge {

    private final ContextRefresher contextRefresher;

    public ConfigRefreshBridge(ContextRefresher contextRefresher) {
        this.contextRefresher = contextRefresher;
    }

    @EventListener
    public void onConfigChange(ConfigChangeEvent event) {
        log.debug("Refreshing Spring Cloud scope for app={}", event.getAppCode());
        try {
            contextRefresher.refresh();
            log.info("@RefreshScope beans refreshed successfully");
        } catch (Exception e) {
            log.error("Failed to refresh @RefreshScope beans", e);
        }
    }
}
