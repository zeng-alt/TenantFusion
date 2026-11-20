package com.github.zeng.alt.event.context;

import com.github.zeng.alt.event.config.EventContextAutoConfiguration;
import com.github.zeng.alt.event.config.ModulithEventContextAutoConfiguration;
import com.github.zeng.alt.event.config.NamastackEventContextAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.context.DelegatingApplicationListener;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 纯 Spring 事件应用无需引入 Modulith 或 Namastack 运行时。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class EventContextAutoConfigurationTest {

    @Test
    void leavesFinalApplicationListenerUnproxied() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(EventContextAutoConfiguration.class))
                .withBean(FinalListener.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(FinalListener.class);
                });
    }

    @Test
    void leavesFinalSpringSecurityListenerUnproxied() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(EventContextAutoConfiguration.class))
                .withBean(DelegatingApplicationListener.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(DelegatingApplicationListener.class);
                });
    }

    @Test
    void leavesPersistenceAdaptersOptional() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader("io.namastack", "org.springframework.modulith"))
                .withConfiguration(AutoConfigurations.of(
                        EventContextAutoConfiguration.class,
                        ModulithEventContextAutoConfiguration.class,
                        NamastackEventContextAutoConfiguration.class
                )).run(context -> {
                    assertThat(context).hasSingleBean(EventContextTemplate.class);
                    assertThat(context).doesNotHaveBean("contextEventSerializerPostProcessor");
                    assertThat(context).doesNotHaveBean("eventOutboxContextProvider");
                });
    }

    /**
     * 模拟第三方提供的不可继承监听器。
     */
    static final class FinalListener implements ApplicationListener<ApplicationEvent> {

        @Override
        public void onApplicationEvent(ApplicationEvent event) {
        }
    }
}
