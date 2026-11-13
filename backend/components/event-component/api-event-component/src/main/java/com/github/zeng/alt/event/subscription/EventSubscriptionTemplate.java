package com.github.zeng.alt.event.subscription;

import com.github.zeng.alt.event.EventException;
import com.github.zeng.alt.event.annotation.EventSubscription;
import com.github.zeng.alt.event.config.EventBindingProperties;
import com.github.zeng.alt.event.context.EventContextTemplate;
import com.github.zeng.alt.event.spi.EventEndpointDto;
import com.github.zeng.alt.event.spi.EventSubscriptionHandler;
import com.github.zeng.alt.event.spi.EventTransportTemplate;
import com.github.zeng.alt.event.support.EventCodec;
import com.github.zeng.alt.event.support.EventTransportResolver;

import io.vavr.control.Option;

import lombok.RequiredArgsConstructor;

import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
import org.springframework.aop.support.AopUtils;
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
import java.lang.reflect.Proxy;
import java.util.Arrays;
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
    private final Map<String, EventSubscriptionHandler> subscriptionById = new LinkedHashMap<>();
    private boolean running;

    @Override
    public void afterSingletonsInstantiated() {
        Arrays.stream(beans.getBeanDefinitionNames()).forEach(this::registerBean);
    }

    private void registerBean(String name) {
        Option.of(AutoProxyUtils.determineTargetClass(beans, name))
                .filter(type -> !type.getName().startsWith("org.springframework."))
                .map(type -> Proxy.isProxyClass(type) ? AopUtils.getTargetClass(beans.getBean(name)) : type)
                .map(
                        type -> MethodIntrospector.selectMethods(
                                type,
                                (MethodIntrospector.MetadataLookup<EventSubscription>) method -> AnnotatedElementUtils
                                        .findMergedAnnotation(method, EventSubscription.class)
                        )
                ).filter(methods -> !methods.isEmpty()).peek(methods -> {
                    // 按用户注解发现监听器时，需要获取实际 Spring 代理，而不是另建目标实例。
                    final Object bean = beans.getBean(name);
                    methods.forEach((method, annotation) -> register(bean, method, annotation));
                });
    }

    private void register(Object bean, Method method, EventSubscription annotation) {
        final String id = beans.resolveEmbeddedValue(annotation.id());
        final String bindingName = beans.resolveEmbeddedValue(annotation.binding());
        if (!StringUtils.hasText(id) || subscriptionById.containsKey(id)) {
            throw new EventException("Duplicate or empty event listener id: " + id);
        }
        final EventBindingProperties binding = transports.binding(bindingName);
        binding.validateSubscription();
        final var endpoint = new EventEndpointDto(id, bindingName, binding);
        final EventTransportTemplate transport = transports.transport(binding.getTransport());
        transport.validate(endpoint);
        final Option<TransactionTemplate> transaction = Option.of(binding.getTransactionManager())
                .filter(StringUtils::hasText)
                .map(name -> new TransactionTemplate(beans.getBean(name, PlatformTransactionManager.class)));
        final var handler = new EventMethodHandler(bean, method, codec, contexts, transaction);
        subscriptionById.put(
                id,
                transport.subscribe(
                        endpoint,
                        (message, nativeContexts) -> handler.invoke(endpoint, message, nativeContexts)
                )
        );
    }

    public Set<String> ids() {
        return Set.copyOf(subscriptionById.keySet());
    }

    /** 按端点 ID 启动订阅，也可启动 auto-startup=false 的端点。 */
    public EventSubscriptionTemplate start(String id) {
        requireSubscription(id).start();
        return this;
    }

    /** 停止指定端点的容器，保留订阅注册以便再次启动。 */
    public EventSubscriptionTemplate stop(String id) {
        requireSubscription(id).stop();
        return this;
    }

    @Override
    public void start() {
        subscriptionById.values().stream().filter(EventSubscriptionHandler::isAutoStartup)
                .forEach(EventSubscriptionHandler::start);
        running = true;
    }

    @Override
    public void stop() {
        subscriptionById.values().forEach(EventSubscriptionHandler::stop);
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void destroy() {
        subscriptionById.values().forEach(EventSubscriptionHandler::destroy);
    }

    private EventSubscriptionHandler requireSubscription(String id) {
        return Option.of(subscriptionById.get(id))
                .getOrElseThrow(() -> new EventException("Unknown event listener: " + id));
    }
}
