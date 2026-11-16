package com.github.zeng.alt.config.client;

import com.github.zeng.alt.config.annotation.ConfigListener;
import com.github.zeng.alt.config.annotation.ConfigValue;
import com.github.zeng.alt.config.annotation.ConfigProperties;
import lombok.Data;
import com.github.zeng.alt.config.client.aot.ConfigBindingAotProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Native Image 配置注入和监听提示测试。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class ConfigBindingAotProcessorTest {
    @Test
    void registersBoundFieldsPayloadTypesAndListenerMethods() {
        final var beans = new DefaultListableBeanFactory();
        beans.registerBeanDefinition("settings", new RootBeanDefinition(Settings.class));
        final var hints = new RuntimeHints();
        final var generation = mock(GenerationContext.class);
        when(generation.getRuntimeHints()).thenReturn(hints);
        new ConfigBindingAotProcessor().processAheadOfTime(RegisteredBean.of(beans, "settings"))
                .applyTo(generation, mock(BeanRegistrationCode.class));

        assertThat(hints.reflection().getTypeHint(Settings.class).fields())
                .anyMatch(field -> field.getName().equals("rules"));
        assertThat(hints.reflection().getTypeHint(Settings.class).methods())
                .anyMatch(method -> method.getName().equals("changed"));
        assertThat(hints.reflection().getTypeHint(Rules.class)).isNotNull();
    }

    @Test
    void registersPlainConfigurationPropertiesAndTheirNestedTypes() {
        final var beans = new DefaultListableBeanFactory();
        beans.registerBeanDefinition("properties", new RootBeanDefinition(Properties.class));
        final var hints = new RuntimeHints();
        final var generation = mock(GenerationContext.class);
        when(generation.getRuntimeHints()).thenReturn(hints);
        new ConfigBindingAotProcessor().processAheadOfTime(RegisteredBean.of(beans, "properties"))
                .applyTo(generation, mock(BeanRegistrationCode.class));
        assertThat(hints.reflection().getTypeHint(Properties.class)).isNotNull();
        assertThat(hints.reflection().getTypeHint(Rules.class)).isNotNull();
    }

    @Data
    @ConfigProperties(prefix = "payment")
    private static class Properties {
        private Rules rules = new Rules(3);
    }

    private record Rules(
        int limit
    ) {
    }

    private static class Settings {
        @ConfigValue("rules")
        private Rules rules;

        @ConfigListener(keys = "rules")
        void changed() {
        }
    }
}
