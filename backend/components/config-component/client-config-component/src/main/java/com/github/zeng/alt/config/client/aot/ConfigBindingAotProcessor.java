package com.github.zeng.alt.config.client.aot;

import com.github.zeng.alt.config.annotation.ConfigListener;
import com.github.zeng.alt.config.annotation.ConfigProperties;
import com.github.zeng.alt.config.annotation.ConfigValue;
import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ReflectionUtils;

/**
 * 为配置字段、结构化字段类型及监听方法注册 Native Image 反射提示。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public class ConfigBindingAotProcessor implements BeanRegistrationAotProcessor {
    @Override
    public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean bean) {
        return (context, code) -> {
            final ReflectionHints hints = context.getRuntimeHints().reflection();
            if (bean.getBeanFactory().findAnnotationOnBean(bean.getBeanName(), ConfigProperties.class, false) != null) {
                new BindingReflectionHintsRegistrar().registerReflectionHints(hints, bean.getBeanClass());
            }
            ReflectionUtils.doWithFields(
                bean.getBeanClass(),
                field -> {
                    if (field.isAnnotationPresent(ConfigValue.class)) {
                        hints.registerField(field);
                        new BindingReflectionHintsRegistrar().registerReflectionHints(hints, field.getGenericType());
                    }
                }
            );
            MethodIntrospector.selectMethods(
                bean.getBeanClass(),
                (MethodIntrospector.MetadataLookup<ConfigListener>) method ->
                        AnnotatedElementUtils.findMergedAnnotation(method, ConfigListener.class)
            )
                    .keySet().forEach(method -> hints.registerMethod(method, ExecutableMode.INVOKE));
        };
    }
}
