package com.github.zeng.alt.security.abac.spel;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.TypedValue;
import org.springframework.core.convert.TypeDescriptor;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 可视化构建器使用的只读字符串/集合操作，逐项分派，不使用反射方法调用。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
final class AbacValueMethodResolver implements MethodResolver {
    static final Set<String> METHODS = Set.of("contains", "containsKey", "startsWith", "endsWith", "isEmpty",
            "size", "length", "toLowerCase", "toUpperCase", "trim", "matches");

    @Override
    public MethodExecutor resolve(EvaluationContext context, Object target, String name, List<TypeDescriptor> arguments) {
        if (!METHODS.contains(name) || !(target instanceof String || target instanceof Collection<?> || target instanceof Map<?, ?>)) {
            return null;
        }
        return (ctx, value, args) -> new TypedValue(invoke(value, name, args));
    }

    private Object invoke(Object value, String name, Object[] args) throws AccessException {
        if (args.length == 0) {
            if (value instanceof String text) {
                return switch (name) {
                    case "isEmpty" -> text.isEmpty();
                    case "length", "size" -> text.length();
                    case "toLowerCase" -> text.toLowerCase(java.util.Locale.ROOT);
                    case "toUpperCase" -> text.toUpperCase(java.util.Locale.ROOT);
                    case "trim" -> text.trim();
                    default -> throw new AccessException("不支持的字符串操作: " + name);
                };
            }
            if (value instanceof Collection<?> values && (name.equals("size") || name.equals("isEmpty"))) {
                return name.equals("size") ? values.size() : values.isEmpty();
            }
            if (value instanceof Map<?, ?> values && (name.equals("size") || name.equals("isEmpty"))) {
                return name.equals("size") ? values.size() : values.isEmpty();
            }
        }
        if (args.length == 1) {
            if (value instanceof Collection<?> values && name.equals("contains")) { return values.contains(args[0]); }
            if (value instanceof Map<?, ?> values && name.equals("containsKey")) { return values.containsKey(args[0]); }
            if (value instanceof String text && args[0] instanceof String expected) {
                return switch (name) {
                    case "contains" -> text.contains(expected);
                    case "startsWith" -> text.startsWith(expected);
                    case "endsWith" -> text.endsWith(expected);
                    case "matches" -> text.matches(expected);
                    default -> throw new AccessException("不支持的字符串操作: " + name);
                };
            }
        }
        throw new AccessException("数据操作参数不匹配: " + name);
    }
}
