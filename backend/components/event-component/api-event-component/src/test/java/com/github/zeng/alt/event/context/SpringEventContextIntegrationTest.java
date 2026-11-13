package com.github.zeng.alt.event.context;

import com.github.zeng.alt.event.config.EventContextAutoConfiguration;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.AopTestUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 不依赖持久化或 TaskDecorator 的 Spring 原生发布入口测试。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class SpringEventContextIntegrationTest {

    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
        UserContextHolder.clear();
    }

    @Test
    void capturesAtPublicationBeforeUndecoratedAsyncExecutorRuns() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(EventContextAutoConfiguration.class))
                .withUserConfiguration(Application.class)
                .run(context -> {
                    TenantContextHolder.setTenantId("publisher");
                    UserContextHolder.set(UsernamePasswordAuthenticationToken.authenticated(
                            SecurityUser.withUsername("alice").id("42").build(), null, List.of()
                    ));
                    context.publishEvent(new PlainEvent("first"));
                    TenantContextHolder.setTenantId("second-publisher");
                    UserContextHolder.clear();
                    context.publishEvent(new PlainEvent("second"));
                    TenantContextHolder.setTenantId("worker");
                    context.getBean(QueuedTaskExecutor.class).runAll();
                    assertThat(context.getBean(RecordingListener.class).observations()).containsExactly(
                            "first:publisher:42", "second:second-publisher:null"
                    );
                    assertThat(TenantContextHolder.getTenantId()).isEqualTo("worker");
                    assertThat(UserContextHolder.getId()).isNull();
                });
    }

    @Test
    void preservesUserConfiguredMulticaster() {
        final var multicaster = new SimpleApplicationEventMulticaster();
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(EventContextAutoConfiguration.class))
                .withBean("applicationEventMulticaster", SimpleApplicationEventMulticaster.class, () -> multicaster)
                .withUserConfiguration(Application.class)
                .run(context -> {
                    assertThat(AopTestUtils.<SimpleApplicationEventMulticaster>getUltimateTargetObject(
                            context.getBean("applicationEventMulticaster")
                    )).isSameAs(multicaster);
                    TenantContextHolder.setTenantId("custom-multicaster");
                    context.publishEvent(new PlainEvent("custom"));
                    clearContext();
                    context.getBean(QueuedTaskExecutor.class).runAll();
                    assertThat(context.getBean(RecordingListener.class).observations())
                            .containsExactly("custom:custom-multicaster:null");
                });
    }

    /**
     * 不带持久化设施的最小 Spring 应用。
     *
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    @Configuration(proxyBeanMethods = false)
    @EnableAsync
    static class Application {

        @Bean
        QueuedTaskExecutor listenerExecutor() {
            return new QueuedTaskExecutor();
        }

        @Bean
        RecordingListener recordingListener() {
            return new RecordingListener();
        }
    }

    /**
     * 读取实际业务 Holder 的普通异步监听器。
     *
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    public static class RecordingListener {

        private final List<String> observations = new ArrayList<>();

        @Async("listenerExecutor")
        @EventListener
        public void onEvent(PlainEvent event) {
            observations.add(event.id() + ":" + TenantContextHolder.getTenantId() + ":" + UserContextHolder.getId());
            TenantContextHolder.setTenantId("changed-by-listener");
        }

        public List<String> observations() {
            return List.copyOf(observations);
        }
    }

    /**
     * 延迟执行且不透传上下文，模拟独立的工作线程状态。
     *
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    static class QueuedTaskExecutor implements TaskExecutor {

        private final Queue<Runnable> tasks = new ArrayDeque<>();

        @Override
        public void execute(Runnable task) {
            tasks.add(task);
        }

        void runAll() {
            tasks.forEach(Runnable::run);
            tasks.clear();
        }
    }

    /**
     * 无上下文字段的业务事件。
     *
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    public record PlainEvent(String id) {
    }
}
