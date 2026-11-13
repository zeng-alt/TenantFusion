package com.github.zeng.alt.event.spi;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.SmartLifecycle;

/**
 * 订阅资源的生命周期句柄，由注册中心统一启停。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public interface EventSubscriptionHandler extends SmartLifecycle, DisposableBean {
    @Override
    default void destroy() {
        stop();
    }
}
