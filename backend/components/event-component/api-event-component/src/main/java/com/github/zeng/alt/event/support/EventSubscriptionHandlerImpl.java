package com.github.zeng.alt.event.support;

import com.github.zeng.alt.event.spi.EventSubscriptionHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.Lifecycle;
import org.springframework.beans.factory.DisposableBean;
import io.vavr.control.Try;
import com.github.zeng.alt.event.EventException;

/**
 * 适配 Spring 原生容器的启停生命周期。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class EventSubscriptionHandlerImpl implements EventSubscriptionHandler {
    private final Lifecycle delegate;
    private final boolean autoStartup;
    @Override public boolean isAutoStartup() { return autoStartup; }
    @Override public void start() { delegate.start(); }
    @Override public void stop() { delegate.stop(); }
    @Override public void destroy() {
        stop();
        if (delegate instanceof DisposableBean disposable) {
            Try.run(disposable::destroy).getOrElseThrow(cause -> new EventException("Cannot destroy subscription", cause));
        }
    }
    @Override public boolean isRunning() { return delegate.isRunning(); }
}
