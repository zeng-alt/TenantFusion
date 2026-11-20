package com.github.zeng.alt.security.abac.method;

import com.github.zeng.alt.security.abac.AbacResourceResolver;
import com.github.zeng.alt.security.abac.spel.SpelAbacEvaluator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.MethodIntrospector;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按权限缓存授权方法目录，为编辑器和服务端校验提供相同元数据。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class AbacMethodResolver implements SmartInitializingSingleton {
    private final ConfigurableListableBeanFactory beans;
    private final AbacAuthorizeAttributeRegistry registry;
    private final ObjectProvider<AbacResourceResolver> resources;
    private final ObjectProvider<AbacSchemaCustomizer> customizers;
    private final ObjectProvider<SpelAbacEvaluator> evaluator;
    private final Map<String, List<AbacAuthorizeAttribute>> methods = new ConcurrentHashMap<>();
    private final AbacTypeResolver types = new AbacTypeResolver();

    public AbacMethodResolver(ConfigurableListableBeanFactory beans, AbacAuthorizeAttributeRegistry registry,
            ObjectProvider<AbacResourceResolver> resources, ObjectProvider<AbacSchemaCustomizer> customizers,
            ObjectProvider<SpelAbacEvaluator> evaluator) {
        this.beans = beans;
        this.registry = registry;
        this.resources = resources;
        this.customizers = customizers;
        this.evaluator = evaluator;
    }

    @Override
    public void afterSingletonsInstantiated() {
        for (String name : beans.getBeanDefinitionNames()) {
            final Class<?> type = beans.getType(name, false);
            if (type == null) { continue; }
            final Class<?> target = ClassUtils.getUserClass(type);
            MethodIntrospector.selectMethods(target, (MethodIntrospector.MetadataLookup<AbacAuthorizeAttribute>) method ->
                    registry.getAttribute(method, target).getOrNull()).values().stream().distinct().forEach(attribute ->
                    methods.compute(attribute.permission(), (key, previous) -> {
                        final List<AbacAuthorizeAttribute> values = new ArrayList<>(previous == null ? List.of() : previous);
                        if (values.stream().noneMatch(value -> value.metadata().method().equals(attribute.metadata().method()))) {
                            values.add(attribute);
                        }
                        return List.copyOf(values);
                    }));
        }
    }

    public AbacSchemaDto getSchema(String permission) {
        final AbacSchemaBuilder builder = new AbacSchemaBuilder();
        customizers.orderedStream().forEach(customizer -> customizer.customize(builder));
        builder.field("principal", "id", String.class).field("principal", "tenant", String.class)
                .field("principal", "username", String.class).field("principal", "enabled", Boolean.class)
                .field("authentication", "name", String.class).field("authentication", "authenticated", Boolean.class)
                .field("authentication", "roles", List.class).field("authentication", "authorities", List.class)
                .field("env", "method", String.class).field("env", "path", String.class).field("env", "hour", Integer.class);
        final List<AbacAuthorizeAttribute> bindings = methods.getOrDefault(permission, List.of());
        bindings.forEach(attribute -> attribute.resources().forEach(type -> resources.orderedStream()
                .filter(resolver -> type.isInstance(resolver)).forEach(resolver -> resolver.attributes()
                        .forEach((name, fieldType) -> builder.field("resource", name, fieldType)))));
        final List<AbacFieldDto> globals = new ArrayList<>(builder.build());
        globals.add(new AbacFieldDto("tenant", "String", false, "服务端上下文", List.of()));
        globals.add(new AbacFieldDto("permission", "String", false, "服务端上下文", List.of()));
        globals.add(new AbacFieldDto("permitAll", "Boolean", false, "常量", List.of()));
        globals.add(new AbacFieldDto("denyAll", "Boolean", false, "常量", List.of()));
        return new AbacSchemaDto(permission,
                globals,
                bindings.stream().map(this::methodSchema).toList(),
                evaluator.orderedStream().flatMap(engine -> engine.functions().stream()).distinct().sorted().toList());
    }

    private AbacMethodSchemaDto methodSchema(AbacAuthorizeAttribute attribute) {
        final AbacMethodDto metadata = attribute.metadata();
        final List<AbacParameterDto> parameters = metadata.argumentIndices().entrySet().stream()
                .map(entry -> new AbacParameterDto(entry.getKey(), entry.getValue(),
                        types.field("#" + entry.getKey(), metadata.parameterTypes().get(entry.getValue()), "方法参数")))
                .toList();
        return new AbacMethodSchemaDto(metadata.targetClass().getSimpleName() + "." + metadata.method().getName(),
                attribute.transactional(), parameters, types.field("returnObject", metadata.returnType(), "查询返回值"));
    }
}
