package com.github.zeng.alt.event.spi;

import com.github.zeng.alt.event.config.EventBindingProperties;

/**
 * 已解析的监听端点。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record EventEndpointDto(
        String id,
        String bindingName,
        EventBindingProperties binding
) {
}
