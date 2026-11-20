package com.github.zeng.alt.security.abac.method;

import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.springframework.beans.BeanUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

/**
 * 将 DTO/record 投影成表达式数据树，缓存属性读方法，不缓存对象值。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class AbacValueResolver {
    private static final Set<String> HIDDEN = Set.of("class", "password", "credentials", "secret", "token",
            "accessToken", "refreshToken");
    private final Map<Class<?>, Map<String, Method>> readers = new ConcurrentHashMap<>();

    /** 解包业务成功值；空值与业务失败保持缺失。 */
    public Option<Object> resolve(Object value) {
        if (value instanceof Either<?, ?> either) {
            return either.isRight() ? resolve(either.get()) : Option.none();
        }
        if (value instanceof Option<?> option) { return option.flatMap(this::resolve); }
        return Option.of(value).map(this::project);
    }

    private Object project(Object value) {
        if (value instanceof String || value instanceof Number || value instanceof Boolean) { return value; }
        if (value instanceof Character || value instanceof Enum<?> || value instanceof TemporalAccessor
                || value instanceof java.util.UUID) { return value.toString(); }
        if (value instanceof Map<?, ?> map) {
            final Map<String, Object> values = new LinkedHashMap<>();
            map.forEach((key, item) -> values.put(String.valueOf(key), resolve(item).getOrNull()));
            return values;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(item -> resolve(item).getOrNull()).toList();
        }
        if (value.getClass().isArray()) {
            return IntStream.range(0, Array.getLength(value))
                    .mapToObj(index -> resolve(Array.get(value, index)).getOrNull()).toList();
        }
        if (value.getClass().getPackageName().startsWith("java.") || value instanceof Class<?>) {
            throw new IllegalArgumentException("不支持的授权参数类型: " + value.getClass().getName());
        }
        final Map<String, Object> values = new LinkedHashMap<>();
        readers.computeIfAbsent(value.getClass(), this::readers).forEach((name, method) -> values.put(name,
                resolve(Try.of(() -> method.invoke(value)).getOrElseThrow(failure ->
                        new IllegalStateException("无法读取授权属性: " + name, failure))).getOrNull()));
        return values;
    }

    private Map<String, Method> readers(Class<?> type) {
        final Map<String, Method> methods = new LinkedHashMap<>();
        for (var descriptor : BeanUtils.getPropertyDescriptors(type)) {
            if (!HIDDEN.contains(descriptor.getName()) && descriptor.getReadMethod() != null) {
                methods.put(descriptor.getName(), descriptor.getReadMethod());
            }
        }
        if (type.isRecord()) {
            for (var component : type.getRecordComponents()) {
                if (!HIDDEN.contains(component.getName())) { methods.put(component.getName(), component.getAccessor()); }
            }
        }
        methods.values().forEach(method -> method.trySetAccessible());
        return Map.copyOf(methods);
    }
}
