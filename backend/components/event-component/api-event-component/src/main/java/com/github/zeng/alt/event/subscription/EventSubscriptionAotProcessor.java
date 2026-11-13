package com.github.zeng.alt.event.subscription;

import com.github.zeng.alt.event.annotation.EventSubscription;

import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 在 AOT 阶段为具体监听方法和声明的负载类型注册反射提示。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public class EventSubscriptionAotProcessor implements BeanRegistrationAotProcessor {
    @Override
    public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean bean) {
        final Map<Method, EventSubscription> methods = MethodIntrospector.selectMethods(
                bean.getBeanClass(),
                (MethodIntrospector.MetadataLookup<EventSubscription>) method -> AnnotatedElementUtils
                        .findMergedAnnotation(method, EventSubscription.class)
        );
        return (context, code) -> methods.keySet().forEach(method -> {
            context.getRuntimeHints().reflection().registerMethod(method, ExecutableMode.INVOKE);
            new BindingReflectionHintsRegistrar()
                    .registerReflectionHints(context.getRuntimeHints().reflection(), method.getGenericParameterTypes());
        });
    }
}
