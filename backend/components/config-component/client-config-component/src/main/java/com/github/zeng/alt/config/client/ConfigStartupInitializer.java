package com.github.zeng.alt.config.client;

import com.github.zeng.alt.config.ConfigException;
import com.github.zeng.alt.config.ConfigSourceTemplate;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Slf4j
@RequiredArgsConstructor
public class ConfigStartupInitializer implements SmartLifecycle {
    private final ConfigRemoteTemplate remote;
    private final ConfigSubscriber subscriber;
    private final ConfigClientProperties properties;
    private final ObjectProvider<ConfigSourceTemplate> localSources;
    private final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    private Option<ScheduledFuture<?>> polling = Option.none();
    private volatile boolean running;

    @Override
    public void start() {
        if (running || !properties.isRefreshEnabled()) {
            return;
        }
        running = true;
        synchronize();
        scheduler.setThreadNamePrefix("config-sync-");
        scheduler.setDaemon(true);
        scheduler.initialize();
        final long interval = localSources.getIfAvailable() == null
                ? properties.getRetryDelayMs() : properties.getRefreshIntervalMs();
        polling = Option.some(scheduler.scheduleWithFixedDelay(
            this::poll,
            Instant.now().plusMillis(interval),
            Duration.ofMillis(interval)
        ));
    }

    @Override
    public void stop() {
        running = false;
        polling.peek(task -> task.cancel(true));
        polling = Option.none();
        scheduler.shutdown();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void poll() {
        // 本地发布由事务提交事件立即通知；定时校准覆盖同数据库其他实例的发布。
        if (localSources.getIfAvailable() == null) {
            Try.run(remote::awaitChanges).onFailure(this::logFailure);
        }
        if (running) {
            synchronize();
        }
    }

    private void synchronize() {
        Try.of(() -> Option.of(localSources.getIfAvailable())
                .map(source -> source.getAll(properties.getAppCode()).getOrElseThrow(ConfigException::new))
                .getOrElse(remote::getAll))
                .onSuccess(items -> {
                    if (running) {
                        subscriber.acceptSnapshot(items);
                    }
                }).onFailure(this::logFailure);
    }

    private void logFailure(Throwable error) {
        log.warn(
            "Config synchronization failed for app={}; retaining cached values ({})",
            properties.getAppCode(),
            error.getClass().getSimpleName()
        );
    }
}
