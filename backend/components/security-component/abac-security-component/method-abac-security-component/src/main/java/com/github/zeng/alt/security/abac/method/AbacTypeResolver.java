package com.github.zeng.alt.security.abac.method;

import io.vavr.control.Either;
import io.vavr.control.Option;
import org.springframework.beans.BeanUtils;
import org.springframework.core.ResolvableType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 从方法泛型元数据生成可补全的参数和返回字段。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
final class AbacTypeResolver {
    AbacFieldDto field(String path, ResolvableType type, String source) {
        return field(path, type, source, new HashSet<>());
    }

    private AbacFieldDto field(String path, ResolvableType type, String source, Set<Class<?>> parents) {
        final Class<?> resolved = type.resolve(Object.class);
        if (resolved == Either.class) { return field(path, type.getGeneric(1), source, parents); }
        if (resolved == Option.class) { return field(path, type.getGeneric(0), source, parents); }
        final List<AbacFieldDto> fields = new ArrayList<>();
        if (Collection.class.isAssignableFrom(resolved)) {
            fields.add(field(path + "[0]", type.as(Collection.class).getGeneric(0), source, parents));
        } else if (resolved.isArray()) {
            fields.add(field(path + "[0]", type.getComponentType(), source, parents));
        } else if (!resolved.getPackageName().startsWith("java.") && parents.add(resolved)) {
            for (var property : BeanUtils.getPropertyDescriptors(resolved)) {
                if (property.getReadMethod() != null && !hidden(property.getName())) {
                    fields.add(field(path + "." + property.getName(),
                            ResolvableType.forMethodReturnType(property.getReadMethod(), resolved), source,
                            new HashSet<>(parents)));
                }
            }
            if (resolved.isRecord()) {
                for (var component : resolved.getRecordComponents()) {
                    if (!hidden(component.getName()) && fields.stream().noneMatch(f -> f.path().equals(path + "." + component.getName()))) {
                        fields.add(field(path + "." + component.getName(),
                                ResolvableType.forType(component.getGenericType()), source, new HashSet<>(parents)));
                    }
                }
            }
        }
        return new AbacFieldDto(path, type.toString(), !resolved.isPrimitive(), source, fields);
    }

    private boolean hidden(String name) {
        return Set.of("class", "password", "credentials", "secret", "token", "accessToken", "refreshToken").contains(name);
    }
}
