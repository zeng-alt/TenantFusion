package com.github.zeng.alt.config.client;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.github.zeng.alt.config.ConfigException;
import com.github.zeng.alt.config.ConfigSubscriptionHandler;
import com.github.zeng.alt.config.ConfigTemplate;
import com.github.zeng.alt.config.annotation.ConfigBean;
import com.github.zeng.alt.config.annotation.ConfigListener;
import com.github.zeng.alt.config.annotation.ConfigValue;
import com.github.zeng.alt.config.event.ConfigChangeEvent;
import com.github.zeng.alt.config.event.ConfigChangeListener;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * 配置字段注入及方法监听器注册；监听器始终通过 Spring Bean 代理调用。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class ConfigAnnotationBeanPostProcessor implements
        BeanPostProcessor,
        SmartInitializingSingleton,
        DisposableBean {
    private final ConfigurableListableBeanFactory beans;
    private final List<ConfigSubscriptionHandler> subscriptions = new ArrayList<>();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String name) {
        ReflectionUtils.doWithFields(
            bean.getClass(),
            field -> {
                final ConfigValue binding = field.getAnnotation(ConfigValue.class);
                if (binding != null) {
                    bind(bean, name, field, binding);
                }
            }
        );
        return bean;
    }

    private void bind(Object bean, String name, Field field, ConfigValue binding) {
        if (!beans.containsBeanDefinition(name)
                || !"businessConfig".equals(beans.getBeanDefinition(name).getScope())
                || Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
            throw new ConfigException("ConfigValue 只能标注 ConfigBean 的非 final 实例字段: " + name + "." + field.getName());
        }
        final ConfigBean owner = beans.findAnnotationOnBean(name, ConfigBean.class, false);
        if (owner == null) {
            throw new ConfigException("ConfigValue 所在 Bean 必须标注 ConfigBean: " + name);
        }
        final String group = StringUtils.hasText(binding.group()) ? binding.group() : owner.group();
        final String content = beans.getBean(ConfigTemplate.class).group(group).opsForValue().getString(binding.value())
                .orElse(() -> ConfigValue.NO_DEFAULT.equals(binding.defaultValue())
                        ? Option.none() : Option.some(binding.defaultValue()))
                .getOrElseThrow(() -> new ConfigException("配置不存在: " + binding.value()));
        final Object value = convert(content, field);
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, bean, value);
    }

    private Object convert(String content, Field field) {
        final ConversionService conversion = ApplicationConversionService.getSharedInstance();
        final Class<?> type = field.getType();
        return Try.of(() -> {
            if (type == boolean.class || type == Boolean.class) {
                if (!"true".equalsIgnoreCase(content.trim()) && !"false".equalsIgnoreCase(content.trim())) {
                    throw new IllegalArgumentException("Expected true or false");
                }
                return Boolean.valueOf(content.trim());
            }
            if (!type.isArray() && !Collection.class.isAssignableFrom(type) && !Map.class.isAssignableFrom(type)
                    && conversion.canConvert(String.class, type)) {
                return conversion.convert(content, type);
            }
            final ObjectMapper mapper = beans.getBeanProvider(ObjectMapper.class)
                    .getIfAvailable(() -> JsonMapper.builder().findAndAddModules().build());
            return mapper.readValue(content, mapper.constructType(field.getGenericType()));
        }).filter(value -> value != null)
                .getOrElseThrow(error -> new ConfigException("无法绑定配置字段: " + field.getDeclaringClass().getName()
                        + "." + field.getName()));
    }

    @Override
    public void afterSingletonsInstantiated() {
        Arrays.stream(beans.getBeanDefinitionNames()).filter(name -> !ScopedProxyUtils.isScopedTarget(name))
                .forEach(this::registerListeners);
    }

    private void registerListeners(String name) {
        final String target = ScopedProxyUtils.getTargetBeanName(name);
        final Class<?> type = beans.getType(beans.containsBeanDefinition(target) ? target : name, false);
        if (type == null || type.getName().startsWith("org.springframework.")) {
            return;
        }
        MethodIntrospector.selectMethods(
            type,
            (MethodIntrospector.MetadataLookup<ConfigListener>) method ->
                    AnnotatedElementUtils.findMergedAnnotation(method, ConfigListener.class)
        )
                .forEach((method, binding) -> registerListener(name, method, binding));
    }

    private void registerListener(String name, Method method, ConfigListener binding) {
        if (!Modifier.isPublic(method.getModifiers()) || Modifier.isStatic(method.getModifiers())
                || method.getParameterCount() > 1
                || (method.getParameterCount() == 1 && method.getParameterTypes()[0] != ConfigChangeEvent.class)
                || binding.keys().length == 0) {
            throw new ConfigException("ConfigListener 需要 keys 和 public 方法，接受零个参数或一个 ConfigChangeEvent: " + method);
        }
        final ConfigChangeListener listener = event -> invoke(name, method, event);
        final ConfigTemplate template = beans.getBean(ConfigTemplate.class).group(binding.group());
        Arrays.stream(binding.keys()).forEach(key -> subscriptions.add(template.subscribe(key, listener)));
    }

    private void invoke(String name, Method method, ConfigChangeEvent event) {
        final Object bean = beans.getBean(name);
        final Method invocable = AopUtils.selectInvocableMethod(method, bean.getClass());
        ReflectionUtils.makeAccessible(invocable);
        ReflectionUtils.invokeMethod(
            invocable,
            bean,
            method.getParameterCount() == 0 ? new Object[0] : new Object[]{event}
        );
    }

    @Override
    public void destroy() {
        subscriptions.forEach(ConfigSubscriptionHandler::close);
    }
}
