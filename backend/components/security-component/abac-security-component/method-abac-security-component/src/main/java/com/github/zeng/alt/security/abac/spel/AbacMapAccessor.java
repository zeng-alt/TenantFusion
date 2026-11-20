package com.github.zeng.alt.security.abac.spel;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;

import java.util.Map;

/**
 * 只读取授权数据树，不使用 Java 对象的反射属性访问。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
final class AbacMapAccessor implements PropertyAccessor {
    @Override
    public Class<?>[] getSpecificTargetClasses() { return new Class<?>[]{Map.class, String.class, java.util.Collection.class}; }
    @Override
    public boolean canRead(EvaluationContext context, Object target, String name) {
        return target instanceof Map<?, ?> map && map.containsKey(name)
                || target instanceof String && name.equals("length")
                || target instanceof java.util.Collection<?> && (name.equals("size") || name.equals("empty"));
    }
    @Override
    public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
        if (target instanceof Map<?, ?> map && map.containsKey(name)) {
            return new TypedValue(map.get(name));
        }
        if (target instanceof String text && name.equals("length")) { return new TypedValue(text.length()); }
        if (target instanceof java.util.Collection<?> values && name.equals("size")) { return new TypedValue(values.size()); }
        if (target instanceof java.util.Collection<?> values && name.equals("empty")) { return new TypedValue(values.isEmpty()); }
        throw new AccessException("授权属性不存在: " + name);
    }
    @Override
    public boolean canWrite(EvaluationContext context, Object target, String name) { return false; }
    @Override
    public void write(EvaluationContext context, Object target, String name, Object value) throws AccessException {
        throw new AccessException("授权属性为只读");
    }
}
