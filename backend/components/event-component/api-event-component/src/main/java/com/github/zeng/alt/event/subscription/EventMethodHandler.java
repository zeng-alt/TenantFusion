package com.github.zeng.alt.event.subscription;

import com.github.zeng.alt.event.EventContext;
import com.github.zeng.alt.event.EventDelivery;
import com.github.zeng.alt.event.EventException;
import com.github.zeng.alt.event.EventOutcome;
import com.github.zeng.alt.event.context.EventContextTemplate;
import com.github.zeng.alt.event.spi.EventEndpointDto;
import com.github.zeng.alt.event.spi.EventMessageDto;
import com.github.zeng.alt.event.support.EventCodec;
import com.github.zeng.alt.event.support.EventContextDto;
import com.github.zeng.alt.event.support.EventDeliveryImpl;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.messaging.handler.annotation.ValueConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 恢复上下文后调用实际 Spring 代理；托管事务提交成功后才返回确认结果。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public class EventMethodHandler {
    private static final Logger LOG = LoggerFactory.getLogger(EventMethodHandler.class);
    private final Object bean;
    private final Method method;
    private final Method declaration;
    private final EventCodec codec;
    private final EventContextTemplate contexts;
    private final Option<TransactionTemplate> transaction;

    public EventMethodHandler(Object bean, Method method, EventCodec codec,
                              EventContextTemplate contexts, Option<TransactionTemplate> transaction) {
        if (method.getReturnType() != void.class && method.getReturnType() != EventOutcome.class) {
            throw new EventException("Event listener must return void or EventOutcome: " + method);
        }
        if (AnnotatedElementUtils.findMergedAnnotation(method, Async.class) != null
                || AnnotatedElementUtils.findMergedAnnotation(AopUtils.getTargetClass(bean), Async.class) != null) {
            throw new EventException("Event listener cannot use @Async: " + method);
        }
        if (Arrays.stream(method.getParameters()).filter(EventMethodHandler::isPayload).count() > 1) {
            throw new EventException("Event listener accepts at most one payload: " + method);
        }
        this.bean = bean;
        this.declaration = method;
        this.method = AopUtils.selectInvocableMethod(method, bean.getClass());
        ReflectionUtils.makeAccessible(this.method);
        this.codec = codec;
        this.contexts = contexts;
        this.transaction = transaction;
    }

    public EventOutcome invoke(EventEndpointDto endpoint, EventMessageDto message, List<Object> nativeContexts) {
        final var policy = endpoint.binding().getRetry();
        final var context = new EventContextDto(endpoint.id(), message.metadata(), nativeContexts);
        final var delivery = new EventDeliveryImpl(context, policy.delay(context.retryCount()));
        try (final var scope = contexts.restore(context.attributes())) {
            final EventOutcome outcome = Try.of(() -> transaction.map(template -> template.execute(status -> {
                final EventOutcome handled = invokeMethod(message, delivery);
                if (!(handled instanceof EventOutcome.Acknowledged)) {
                    status.setRollbackOnly();
                }
                return handled;
            })).getOrElse(() -> invokeMethod(message, delivery)))
                    .getOrElseGet(failure -> {
                        LOG.warn("Event listener {} failed for message {}", endpoint.id(), context.messageId(), failure);
                        return new EventOutcome.Retry(policy.delay(context.retryCount()), failure.getClass().getSimpleName());
                    });
            if (outcome instanceof EventOutcome.Retry retry && context.retryCount() + 1 >= policy.getMaxAttempts()) {
                return new EventOutcome.Rejected("Retry budget exhausted: " + retry.reason());
            }
            return outcome;
        }
    }

    private EventOutcome invokeMethod(EventMessageDto message, EventDelivery delivery) {
        final Object[] arguments = Arrays.stream(declaration.getParameters())
                .map(parameter -> argument(parameter, message, delivery)).toArray();
        final Object returned = ReflectionUtils.invokeMethod(method, bean, arguments);
        if (method.getReturnType() == void.class) {
            return delivery.ack();
        }
        return Option.of(returned).map(EventOutcome.class::cast)
                .getOrElseThrow(() -> new EventException("EventOutcome must not be null: " + declaration));
    }

    private Object argument(Parameter parameter, EventMessageDto message, EventDelivery delivery) {
        if (parameter.getType() == EventDelivery.class) {
            return delivery;
        }
        if (parameter.getType() == EventContext.class) {
            return delivery.context();
        }
        if (parameter.isAnnotationPresent(Header.class)) {
            final Header header = parameter.getAnnotation(Header.class);
            final String name = StringUtils.hasText(header.name()) ? header.name() : header.value();
            final Option<String> value = Option.of(message.metadata().headers().get(name))
                    .orElse(() -> Option.of(header.defaultValue())
                            .filter(defaultValue -> !defaultValue.equals(ValueConstants.DEFAULT_NONE)));
            if (value.isEmpty() && header.required()) {
                throw new EventException("Missing event header: " + name);
            }
            return value.map(text -> DefaultConversionService.getSharedInstance()
                    .convert(text, parameter.getType())).getOrNull();
        }
        if (parameter.isAnnotationPresent(Headers.class)) {
            return message.metadata().headers();
        }
        if (parameter.getType() == Message.class) {
            final var type = ResolvableType.forType(parameter.getParameterizedType()).getGeneric(0).getType();
            return new GenericMessage<>(codec.payload(message, type), Map.copyOf(message.metadata().headers()));
        }
        return codec.payload(message, parameter.getParameterizedType());
    }

    private static boolean isPayload(Parameter parameter) {
        return parameter.getType() != EventContext.class && parameter.getType() != EventDelivery.class
                && !parameter.isAnnotationPresent(Header.class) && !parameter.isAnnotationPresent(Headers.class);
    }
}
