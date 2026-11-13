package com.github.zeng.alt.event.config;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 事件发送和订阅统一使用的逻辑绑定配置。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Getter
@Setter
@ConfigurationProperties("alt.event")
public class EventProperties {
    private Map<String, EventBindingProperties> bindings = new LinkedHashMap<>();
}
