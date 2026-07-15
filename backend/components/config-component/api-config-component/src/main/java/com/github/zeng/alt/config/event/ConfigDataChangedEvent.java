package com.github.zeng.alt.config.event;

import com.github.zeng.alt.config.model.ConfigItemDTO;
import org.springframework.context.ApplicationEvent;

import java.util.List;

public class ConfigDataChangedEvent extends ApplicationEvent {

    private final String appCode;
    private final List<ConfigItemDTO> items;

    public ConfigDataChangedEvent(Object source, String appCode, List<ConfigItemDTO> items) {
        super(source);
        this.appCode = appCode;
        this.items = items;
    }

    public String getAppCode() {
        return appCode;
    }

    public List<ConfigItemDTO> getItems() {
        return items;
    }
}
