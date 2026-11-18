package com.github.zeng.alt.security.data.jpa;

import com.github.zeng.alt.security.data.DataPermission;
import io.vavr.control.Option;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;

/**
 * 解析接口、实现方法和类上的权限注解。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
final class DataPermissionMethodHelper {
    private DataPermissionMethodHelper() { }

    static Option<DataPermission> resolve(Method method, Class<?> target) {
        final Method specific = AopUtils.getMostSpecificMethod(method, target);
        return Option.of(AnnotatedElementUtils.findMergedAnnotation(specific, DataPermission.class))
                .orElse(() -> Option.of(AnnotatedElementUtils.findMergedAnnotation(method, DataPermission.class)))
                .orElse(() -> Option.of(AnnotatedElementUtils.findMergedAnnotation(target, DataPermission.class)));
    }
}
