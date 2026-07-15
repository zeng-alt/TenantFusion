package com.github.zeng.alt.config.event;

@FunctionalInterface
public interface ConfigChangeListener {

    void onChange(ConfigChangeEvent event);
}
