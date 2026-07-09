package com.github.zeng.alt.message.subscription;

import com.github.zeng.alt.message.Message;
import com.github.zeng.alt.message.MessageHandler;
import com.github.zeng.alt.message.MessageQueueTemplate;
import com.github.zeng.alt.message.annotation.MessageListener;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.log.LogMessage;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringValueResolver;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 在所有单例 Bean 初始化完成后，自动注册消息订阅。
 * <p>
 * Registers message subscriptions after all singleton beans are initialized.
 * <p>
 * 支持两种模式：
 * <ol>
 *   <li><b>注解模式</b> — {@link MessageListener @MessageListener} 标注的方法</li>
 *   <li><b>接口模式</b> — 实现 {@link MessageHandler} 接口的 Spring Bean</li>
 * </ol>
 * <p>
 * 使用 {@link SmartInitializingSingleton} 确保所有 Bean 都已初始化，
 * 此时 {@link MessageQueueTemplate} 一定可用。
 * <p>
 * <b>AOT / GraalVM Native Image 兼容:</b>
 * <ul>
 *   <li>注解已通过 {@code MessageRuntimeHints} 注册</li>
 *   <li>方法调用使用 Spring {@link ReflectionUtils}（兼容 AOT 生成的反射配置）</li>
 *   <li>接口模式通过类型安全的 {@code MessageHandler.onMessage()} 调用，无需反射</li>
 *   <li>自身通过自动配置注册，Spring AOT 可处理</li>
 * </ul>
 *
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
@CommonsLog
@SuppressWarnings("unchecked")
public class MessageListenerBeanPostProcessor implements SmartInitializingSingleton, EmbeddedValueResolverAware, Ordered {

    private final ListableBeanFactory beanFactory;
    private final MessageQueueTemplate messageQueueTemplate;
    private final AtomicInteger subscriberCount = new AtomicInteger(0);
    private StringValueResolver embeddedValueResolver;

    public MessageListenerBeanPostProcessor(ListableBeanFactory beanFactory, MessageQueueTemplate messageQueueTemplate) {
        this.beanFactory = beanFactory;
        this.messageQueueTemplate = messageQueueTemplate;
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.embeddedValueResolver = resolver;
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }

    @Override
    public void afterSingletonsInstantiated() {
        // ========== 1. 接口模式：MessageHandler 实现类 ==========
        Map<String, MessageHandler> handlers = beanFactory.getBeansOfType(MessageHandler.class);
        for (Map.Entry<String, MessageHandler> entry : handlers.entrySet()) {
            if (entry.getValue() == this || entry.getValue() == messageQueueTemplate) {
                continue;
            }
            registerMessageHandler(entry.getKey(), entry.getValue());
        }

        // ========== 2. 注解模式：@MessageListener 标注的方法 ==========
        String[] beanNames = beanFactory.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object bean = beanFactory.getBean(beanName);
            if (bean instanceof MessageHandler || bean == this || bean == messageQueueTemplate) {
                continue;
            }
            processAnnotationMethods(bean);
        }

        log.info(LogMessage.format("Message subscription registration complete, total: %s subscribers", subscriberCount.get()));
    }

    // ========================================================================
    // 接口模式
    // ========================================================================

    /**
     * 注册 {@link MessageHandler} 实现 Bean。
     */
    private <T> void registerMessageHandler(String beanName, MessageHandler<T> handler) {
        String topic = handler.getTopic();
        if (topic == null || topic.isEmpty()) {
            log.warn(LogMessage.format("MessageHandler [%s] returns empty topic, skipping", beanName));
            return;
        }

        messageQueueTemplate.subscribe(topic, handler::onMessage);

        int count = subscriberCount.incrementAndGet();
        log.info(LogMessage.format("Registered MessageHandler [%s] -> topic '%s' (total subscribers: %s)",
                handler.getClass().getSimpleName(), topic, count));
    }

    // ========================================================================
    // 注解模式
    // ========================================================================

    /**
     * 处理单个 Bean，查找标注了 {@link MessageListener} 的方法。
     */
    private void processAnnotationMethods(Object bean) {
        Class<?> clazz = bean.getClass();

        ReflectionUtils.doWithMethods(clazz, method -> {
            MessageListener annotation = AnnotatedElementUtils.findMergedAnnotation(
                    method, MessageListener.class);
            if (annotation == null) {
                return;
            }

            String topic = resolveTopic(annotation.topic());
            if (topic == null || topic.isEmpty()) {
                log.warn(LogMessage.format("@MessageListener on %s.%s has empty topic, skipping",
                        clazz.getSimpleName(), method.getName()));
                return;
            }

            registerAnnotatedSubscription(bean, method, topic);
        }, ReflectionUtils.USER_DECLARED_METHODS);
    }

    /**
     * 为 {@link MessageListener} 标注的方法注册消息订阅。
     */
    private void registerAnnotatedSubscription(Object bean, Method method, String topic) {
        ReflectionUtils.makeAccessible(method);

        // 解析方法参数类型，确定负载类型
        Class<?>[] paramTypes = method.getParameterTypes();
        Class<?> payloadType = resolvePayloadType(method, paramTypes);

        // 通过 MessageQueueTemplate 订阅
        messageQueueTemplate.subscribe(topic, message -> {
            try {
                Object[] args = buildMethodArguments(message, paramTypes, payloadType);
                ReflectionUtils.invokeMethod(method, bean, args);
            } catch (Exception e) {
                log.error(LogMessage.format("Error invoking @MessageListener %s.%s for topic '%s'",
                        bean.getClass().getSimpleName(), method.getName(), topic), e);
            }
        });

        int count = subscriberCount.incrementAndGet();
        log.info(LogMessage.format("Registered @MessageListener [%s.%s] -> topic '%s' (total subscribers: %s)",
                bean.getClass().getSimpleName(), method.getName(), topic, count));
    }

    /**
     * 解析方法参数，构建调用参数数组。
     */
    private Object[] buildMethodArguments(Message<?> message, Class<?>[] paramTypes, Class<?> payloadType) {
        if (paramTypes.length == 0) {
            return new Object[0];
        }

        Object[] args = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> paramType = paramTypes[i];
            if (Message.class.isAssignableFrom(paramType)) {
                // 参数类型是 Message -> 传入完整消息
                args[i] = message;
            } else if (message.getPayload() != null && paramType.isAssignableFrom(message.getPayload().getClass())) {
                // 参数类型匹配负载类型 -> 传入负载
                args[i] = message.getPayload();
            } else {
                log.warn(LogMessage.format("Cannot resolve parameter type %s for @MessageListener, passing null", paramType));
                args[i] = null;
            }
        }
        return args;
    }

    /**
     * 从方法签名中解析负载类型。
     */
    private Class<?> resolvePayloadType(Method method, Class<?>[] paramTypes) {
        for (int i = 0; i < paramTypes.length; i++) {
            if (!Message.class.isAssignableFrom(paramTypes[i])) {
                return paramTypes[i];
            }
            Type genericType = method.getGenericParameterTypes()[i];
            if (genericType instanceof ParameterizedType pt) {
                Type[] actualArgs = pt.getActualTypeArguments();
                if (actualArgs.length > 0 && actualArgs[0] instanceof Class<?> typeArg) {
                    return typeArg;
                }
            }
        }
        return Object.class;
    }

    /**
     * 解析主题，支持占位符 {@code ${...}}。
     */
    private String resolveTopic(String topic) {
        if (embeddedValueResolver != null && topic.contains("${")) {
            return embeddedValueResolver.resolveStringValue(topic);
        }
        return topic;
    }
}
