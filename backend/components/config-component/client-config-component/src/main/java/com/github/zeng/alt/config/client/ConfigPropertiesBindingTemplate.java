package com.github.zeng.alt.config.client;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.github.zeng.alt.config.ConfigException;
import com.github.zeng.alt.config.annotation.ConfigProperties;
import com.github.zeng.alt.config.model.ConfigItemDTO;
import io.vavr.control.Try;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * 使用 Spring Boot Binder 绑定完整前缀，保留字段默认值和本地配置回退。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class ConfigPropertiesBindingTemplate implements InitializingBean, DisposableBean {
    private final ConfigCacheManager cache;
    private final ConfigurableEnvironment environment;
    private final ObjectMapper mapper;
    private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();

    @Override
    public void afterPropertiesSet() {
        validator.afterPropertiesSet();
    }

    public void bind(Object bean, ConfigProperties binding) {
        final Map<String, Object> values = new LinkedHashMap<>();
        cache.getSnapshot().values().stream().filter(item -> binding.group().equals(item.getGroup()))
                .filter(item -> ConfigPropertiesHelper.matches(binding.prefix(), item.getDataId()))
                .sorted(Comparator.comparingInt(item -> item.getDataId().length()))
                .forEach(item -> add(values, item));
        final var sources = Stream.concat(
            Stream.of(new MapConfigurationPropertySource(values)),
            StreamSupport.stream(ConfigurationPropertySources.get(environment).spliterator(), false)
        ).toList();
        Try.run(() -> new Binder(sources, new PropertySourcesPlaceholdersResolver(environment))
                .bind(binding.prefix(), Bindable.ofInstance(bean)))
                .getOrElseThrow(error -> new ConfigException("配置绑定失败: " + (error instanceof BindException failure
                        ? failure.getName() : binding.prefix())));
        validate(bean, binding.prefix());
    }

    private void validate(Object bean, String prefix) {
        final Validated validation = AnnotatedElementUtils.findMergedAnnotation(bean.getClass(), Validated.class);
        if (validation == null) {
            return;
        }
        final var errors = new BeanPropertyBindingResult(bean, prefix);
        validator.validate(bean, errors, (Object[]) validation.value());
        if (errors.hasErrors()) {
            throw new ConfigException("配置校验失败: " + prefix);
        }
    }

    private void add(Map<String, Object> values, ConfigItemDTO item) {
        if (!"json".equalsIgnoreCase(item.getFormat())) {
            values.put(item.getDataId(), item.getContent());
            return;
        }
        final JsonNode node = Try.of(() -> mapper.readTree(item.getContent())).filter(value -> value != null)
                .getOrElseThrow(error -> new ConfigException("JSON 配置格式错误: " + item.getDataId()));
        flatten(values, item.getDataId(), node);
    }

    private void flatten(Map<String, Object> values, String key, JsonNode node) {
        if (node.isObject()) {
            if (node.isEmpty()) {
                values.put(key, Map.of());
            }
            node.properties().forEach(entry -> flatten(values, key + "." + entry.getKey(), entry.getValue()));
            return;
        }
        if (node.isArray()) {
            if (node.isEmpty()) {
                values.put(key, List.of());
            }
            IntStream.range(0, node.size()).forEach(index -> flatten(values, key + "[" + index + "]", node.get(index)));
            return;
        }
        if (!node.isNull()) {
            values.put(
                key,
                node.isTextual() ? node.textValue() : node.isBoolean() ? node.booleanValue() : node.numberValue()
            );
        }
    }

    @Override
    public void destroy() {
        validator.destroy();
    }
}
