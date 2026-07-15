package com.github.zeng.alt.config.client;

import com.github.zeng.alt.config.event.ConfigChangeEvent;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.event.EventListener;

@CommonsLog
@ConditionalOnClass(ContextRefresher.class)
public class ConfigRefreshBridge {

    private final ContextRefresher contextRefresher;

    public ConfigRefreshBridge(ContextRefresher contextRefresher) {
        this.contextRefresher = contextRefresher;
    }

    @EventListener
    public void onConfigChange(ConfigChangeEvent event) {
        log.info("Config changed for appCode=" + event.getAppCode()
                + ", refreshing @RefreshScope beans. Changed keys: " + event.getChangedKeys());
        try {
            contextRefresher.refresh();
            log.info("@RefreshScope beans refreshed successfully");
        } catch (Exception e) {
            log.error("Failed to refresh @RefreshScope beans", e);
        }
    }
}
