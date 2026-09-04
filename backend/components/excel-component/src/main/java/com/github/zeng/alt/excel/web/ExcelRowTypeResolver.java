package com.github.zeng.alt.excel.web;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 从方法参数或返回值的泛型实参解析行类型，按 handler method 缓存。
 * <p>
 * {@code ResolvableType} 要读泛型签名，是反射动作；handler method 的数量有限且固定，
 * 缓存后每个位置只解析一次，而不是每次请求都解析。Servlet 与 WebFlux 两条集成
 * 共用本类。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public class ExcelRowTypeResolver {

    private final Map<MethodParameter, Class<?>> cache = new ConcurrentHashMap<>();

    /**
     * 解析第一个泛型实参。
     *
     * @param parameter 方法参数或返回值
     * @param hint      报错时提示用的注解名
     * @return 行类型
     * @throws IllegalArgumentException 泛型被擦除、推断不出行类型
     */
    public Class<?> resolve(MethodParameter parameter, String hint) {
        Class<?> rowType = cache.computeIfAbsent(parameter, ExcelRowTypeResolver::doResolve);
        if (rowType == Void.class) {
            throw new IllegalArgumentException("%s 位置 '%s' 缺少泛型实参，无法确定行类型"
                    .formatted(hint, parameter.getParameterName()));
        }
        return rowType;
    }

    /**
     * 解析第一个泛型实参，推断不出时返回 {@code null}。
     *
     * @param parameter 方法参数或返回值
     * @return 行类型或 {@code null}
     */
    public Class<?> resolveOrNull(MethodParameter parameter) {
        Class<?> rowType = cache.computeIfAbsent(parameter, ExcelRowTypeResolver::doResolve);
        return rowType == Void.class ? null : rowType;
    }

    /** {@code Void.class} 当作「推断失败」的哨兵，因为 ConcurrentHashMap 不收 null 值 */
    private static Class<?> doResolve(MethodParameter parameter) {
        Class<?> resolved = ResolvableType.forMethodParameter(parameter).getGeneric(0).resolve();
        return resolved == null ? Void.class : resolved;
    }
}
