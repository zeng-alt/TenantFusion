package com.github.zeng.alt.config.event;

import com.github.zeng.alt.config.model.ConfigChangedKey;
import java.util.List;
import org.springframework.context.ApplicationEvent;

public class ConfigChangeEvent extends ApplicationEvent {

    private final String appCode;
    private final List<ConfigChangedKey> changedKeys;

    public ConfigChangeEvent(Object source, String appCode, List<ConfigChangedKey> changedKeys) {
        super(source);
        this.appCode = appCode;
        this.changedKeys = changedKeys;
    }

    public String getAppCode() {
        return appCode;
    }

    public List<ConfigChangedKey> getChangedKeys() {
        return changedKeys;
    }
}
