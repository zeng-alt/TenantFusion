package com.github.zeng.alt.event.config;

import com.github.zeng.alt.event.context.EventContextTemplate;
import com.github.zeng.alt.event.internal.EventContextHelper;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.EventListener;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;

/**
 * 在所有原生 Spring 发布进入广播器时捕获快照，保留现有广播器的持久化及分发行为。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class SpringEventContextConfiguration {

    @Bean
    public static BeanFactoryPostProcessor eventMulticasterRegistration() {
        return beanFactory -> {
            final String name = AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME;
            if (beanFactory.containsLocalBean(name)) {
                return;
            }
            // Spring 默认广播器由容器直接创建，不经过 AOP；注册同类型 Bean 让发布边界可被装饰。
            final var definition = new RootBeanDefinition(SimpleApplicationEventMulticaster.class);
            definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            definition.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
            ((BeanDefinitionRegistry) beanFactory).registerBeanDefinition(name, definition);
        };
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public Advisor eventPublicationContextAdvisor(EventContextTemplate contexts, EventContextHelper snapshots) {
        final var pointcut = new StaticMethodMatcherPointcut() {
            @Override
            public boolean matches(Method method, Class<?> targetClass) {
                return ApplicationEventMulticaster.class.isAssignableFrom(targetClass)
                        && method.getName().equals("multicastEvent");
            }
        };
        final MethodInterceptor interceptor = invocation -> {
            final var snapshotByKey = snapshots.capture(invocation.getArguments()[0]);
            try (final var scope = contexts.restore(snapshotByKey)) {
                return invocation.proceed();
            }
        };
        final var advisor = new DefaultPointcutAdvisor(pointcut, interceptor);
        advisor.setOrder(Ordered.HIGHEST_PRECEDENCE + 100);
        return advisor;
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public Advisor eventContextAdvisor(EventContextTemplate contexts, EventContextHelper snapshots) {
        final var pointcut = new StaticMethodMatcherPointcut() {
            @Override
            public boolean matches(Method method, Class<?> targetClass) {
                return method.getParameterCount() == 1
                        && (AnnotatedElementUtils.hasAnnotation(
                                AopUtils.getMostSpecificMethod(method, targetClass), EventListener.class
                        ) || (ApplicationListener.class.isAssignableFrom(targetClass)
                                && method.getName().equals("onApplicationEvent")));
            }
        };
        final MethodInterceptor interceptor = invocation -> {
            try (final var scope = contexts.restore(snapshots.getContext(invocation.getArguments()[0]))) {
                return invocation.proceed();
            }
        };
        final var advisor = new DefaultPointcutAdvisor(pointcut, interceptor);
        advisor.setOrder(Ordered.HIGHEST_PRECEDENCE + 100);
        return advisor;
    }
}
