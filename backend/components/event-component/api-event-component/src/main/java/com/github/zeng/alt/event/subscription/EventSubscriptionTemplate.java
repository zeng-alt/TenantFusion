package com.github.zeng.alt.event.subscription;

import com.github.zeng.alt.event.EventException;
import com.github.zeng.alt.event.annotation.EventSubscription;
import com.github.zeng.alt.event.context.EventContextTemplate;
import com.github.zeng.alt.event.spi.EventEndpointDto;
import com.github.zeng.alt.event.spi.EventSubscriptionHandler;
import com.github.zeng.alt.event.support.EventCodec;
import com.github.zeng.alt.event.support.EventTransportResolver;
import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 按端点 ID 管理订阅；方法发现和调用使用 Spring 类型信息与代理。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class EventSubscriptionTemplate implements SmartInitializingSingleton, SmartLifecycle, DisposableBean {
    private final ConfigurableListableBeanFactory beans;
    private final EventTransportResolver transports;
    private final EventCodec codec;
    private final EventContextTemplate contexts;
    private final Map<String, EventSubscriptionHandler> subscriptions = new LinkedHashMap<>();
    private boolean running;

    @Override
    public void afterSingletonsInstantiated() {
        for (String name : beans.getBeanDefinitionNames()) {
            final Class<?> type = AutoProxyUtils.determineTargetClass(beans, name);
            if (type == null || type.getName().startsWith("org.springframework.")) {
                continue;
            }
            final Class<?> targetType = java.lang.reflect.Proxy.isProxyClass(type)
                    ? AopUtils.getTargetClass(beans.getBean(name)) : type;
            final Map<Method, EventSubscription> methods = MethodIntrospector.selectMethods(targetType,
                    (MethodIntrospector.MetadataLookup<EventSubscription>) method ->
                            AnnotatedElementUtils.findMergedAnnotation(method, EventSubscription.class));
            if (!methods.isEmpty()) {
                final Object bean = beans.getBean(name);
                methods.forEach((method, annotation) -> register(bean, method, annotation));
            }
        }
    }

    private void register(Object bean, Method method, EventSubscription annotation) {
        final String id = beans.resolveEmbeddedValue(annotation.id());
        final String bindingName = beans.resolveEmbeddedValue(annotation.binding());
        if (!StringUtils.hasText(id) || subscriptions.containsKey(id)) {
            throw new EventException("Duplicate or empty event listener id: " + id);
        }
        final var binding = transports.binding(bindingName);
        binding.validateSubscription();
        final var endpoint = new EventEndpointDto(id, bindingName, binding);
        final var transport = transports.transport(binding.getTransport());
        transport.validate(endpoint);
        final Option<TransactionTemplate> transaction = Option.of(binding.getTransactionManager())
                .filter(StringUtils::hasText).map(name -> new TransactionTemplate(
                        beans.getBean(name, PlatformTransactionManager.class)));
        final var handler = new EventMethodHandler(bean, method, codec, contexts, transaction);
        subscriptions.put(id, transport.subscribe(endpoint,
                (message, nativeContexts) -> handler.invoke(endpoint, message, nativeContexts)));
    }

    public Set<String> ids() { return Set.copyOf(subscriptions.keySet()); }
    public EventSubscriptionTemplate start(String id) { requireSubscription(id).start(); return this; }
    public EventSubscriptionTemplate stop(String id) { requireSubscription(id).stop(); return this; }
    @Override public void start() {
        subscriptions.values().stream().filter(EventSubscriptionHandler::isAutoStartup)
                .forEach(EventSubscriptionHandler::start);
        running = true;
    }
    @Override public void stop() {
        subscriptions.values().forEach(EventSubscriptionHandler::stop);
        running = false;
    }
    @Override public boolean isRunning() { return running; }
    @Override public void destroy() { subscriptions.values().forEach(EventSubscriptionHandler::destroy); }
    private EventSubscriptionHandler requireSubscription(String id) {
        return Option.of(subscriptions.get(id))
                .getOrElseThrow(() -> new EventException("Unknown event listener: " + id));
    }
}
