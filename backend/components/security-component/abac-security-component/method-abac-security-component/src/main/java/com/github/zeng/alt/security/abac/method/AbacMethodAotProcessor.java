package com.github.zeng.alt.security.abac.method;

import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 构建时发现授权方法，注册方法及泛型 DTO 反射提示，并固化参数名供 Native 运行时读取。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public class AbacMethodAotProcessor implements BeanFactoryInitializationAotProcessor {
    @Override
    public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beans) {
        final AbacAuthorizeAttributeRegistry registry = new AbacAuthorizeAttributeRegistry();
        final List<AbacMethodDto> methods = new ArrayList<>();
        for (String name : beans.getBeanDefinitionNames()) {
            final Class<?> type = beans.getType(name, false);
            if (type != null) {
                final Class<?> userClass = ClassUtils.getUserClass(type);
                methods.addAll(MethodIntrospector.selectMethods(userClass,
                                (MethodIntrospector.MetadataLookup<AbacAuthorizeAttribute>) method ->
                                        registry.getAttribute(method, userClass).getOrNull())
                        .values().stream().map(AbacAuthorizeAttribute::metadata).toList());
            }
        }
        if (methods.isEmpty()) {
            return null;
        }
        return (context, code) -> {
            final ReflectionHints hints = context.getRuntimeHints().reflection();
            new BindingReflectionHintsRegistrar().registerReflectionHints(hints,
                    AbacSchemaDto.class, AbacMethodSchemaDto.class, AbacParameterDto.class, AbacFieldDto.class);
            final Set<ResolvableType> visited = new HashSet<>();
            methods.forEach(metadata -> register(metadata, hints, visited));
            context.getGeneratedFiles().addResourceFile(AbacParameterNameResolver.RESOURCE, parameterNames(methods));
            context.getRuntimeHints().resources().registerPattern(AbacParameterNameResolver.RESOURCE);
        };
    }

    private void register(AbacMethodDto metadata, ReflectionHints hints, Set<ResolvableType> visited) {
        hints.registerType(metadata.targetClass(), MemberCategory.INTROSPECT_PUBLIC_METHODS,
                MemberCategory.INTROSPECT_DECLARED_METHODS);
        hints.registerMethod(metadata.method(), ExecutableMode.INVOKE);
        metadata.parameterTypes().forEach(type -> registerType(type, hints, visited));
        registerType(metadata.returnType(), hints, visited);
    }

    private void registerType(ResolvableType type, ReflectionHints hints, Set<ResolvableType> visited) {
        if (!visited.add(type)) {
            return;
        }
        final Class<?> resolved = type.resolve();
        if (resolved != null) {
            new BindingReflectionHintsRegistrar().registerReflectionHints(hints, resolved);
        }
        for (ResolvableType generic : type.getGenerics()) {
            registerType(generic, hints, visited);
        }
    }

    private String parameterNames(List<AbacMethodDto> methods) {
        final Map<String, String> names = new TreeMap<>();
        final var discoverer = new DefaultParameterNameDiscoverer();
        methods.forEach(metadata -> {
            final Method method = metadata.method();
            final String[] parameters = discoverer.getParameterNames(method);
            if (parameters != null) {
                names.put(AbacParameterNameResolver.key(method), String.join(",", parameters));
            }
        });
        return names.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("\n"));
    }
}
