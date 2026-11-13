package com.github.zeng.alt.event.subscription;

import com.github.zeng.alt.event.annotation.EventSubscription;
import com.github.zeng.alt.event.spi.EventMessageDto;
import com.github.zeng.alt.event.support.EventRuntimeHints;
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
 * Native Image 提示必须包含具体业务负载和监听方法，而不只是注解类型。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class EventSubscriptionAotProcessorTest {
    @Test
    void registersPayloadAndEnvelopeReflectionHints() {
        final var hints = new RuntimeHints();
        new EventRuntimeHints().registerHints(hints, getClass().getClassLoader());
        final var beans = new DefaultListableBeanFactory();
        beans.registerBeanDefinition("listener", new RootBeanDefinition(Listener.class));
        final var contribution = new EventSubscriptionAotProcessor().processAheadOfTime(RegisteredBean.of(beans, "listener"));
        final var generation = mock(GenerationContext.class);
        when(generation.getRuntimeHints()).thenReturn(hints);
        contribution.applyTo(generation, mock(BeanRegistrationCode.class));
        assertThat(hints.reflection().getTypeHint(Payload.class)).isNotNull();
        assertThat(hints.reflection().getTypeHint(EventMessageDto.class)).isNotNull();
        assertThat(hints.reflection().getTypeHint(Listener.class).methods()).anyMatch(method -> method.getName().equals("receive"));
    }

    /**
     * 测试负载。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    public record Payload(String id) { }

    /**
     * 测试监听器。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    public static class Listener {
        @EventSubscription(id = "aot", binding = "orders")
        public void receive(Payload event) { }
    }
}
