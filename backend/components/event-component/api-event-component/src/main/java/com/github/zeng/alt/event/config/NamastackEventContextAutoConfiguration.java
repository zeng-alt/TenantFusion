package com.github.zeng.alt.event.config;

import com.github.zeng.alt.event.context.EventContextTemplate;
import io.namastack.outbox.OutboxRecord;
import io.namastack.outbox.context.OutboxContextProvider;
import io.namastack.outbox.handler.invoker.OutboxFallbackHandlerInvoker;
import io.namastack.outbox.handler.invoker.OutboxHandlerInvoker;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;

import java.lang.reflect.Method;

/**
 * 使用 Namastack 原生 context 列，在其可代理的 dispatch 边界恢复所有 Handler 的上下文。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@AutoConfiguration(after = EventContextAutoConfiguration.class)
@ConditionalOnClass({OutboxContextProvider.class, OutboxHandlerInvoker.class})
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class NamastackEventContextAutoConfiguration {

    @Bean
    public OutboxContextProvider eventOutboxContextProvider(EventContextTemplate contexts) {
        return contexts::capture;
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public Advisor namastackEventContextAdvisor(EventContextTemplate contexts) {
        final var pointcut = new StaticMethodMatcherPointcut() {
            @Override
            public boolean matches(Method method, Class<?> targetClass) {
                return (OutboxHandlerInvoker.class.isAssignableFrom(targetClass)
                        || OutboxFallbackHandlerInvoker.class.isAssignableFrom(targetClass))
                        && method.getName().equals("dispatch") && method.getParameterCount() == 1
                        && method.getParameterTypes()[0] == OutboxRecord.class;
            }
        };
        final MethodInterceptor interceptor = invocation -> {
            final var record = (OutboxRecord<?>) invocation.getArguments()[0];
            try (final var scope = contexts.restore(record.getContext())) {
                return invocation.proceed();
            }
        };
        final var advisor = new DefaultPointcutAdvisor(pointcut, interceptor);
        advisor.setOrder(Ordered.HIGHEST_PRECEDENCE + 100);
        return advisor;
    }
}
