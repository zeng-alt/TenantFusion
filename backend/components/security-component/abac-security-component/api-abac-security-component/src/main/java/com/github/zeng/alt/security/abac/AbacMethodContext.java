package com.github.zeng.alt.security.abac;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 单次方法授权的可信上下文：被拦截方法、目标类、实参、自动注入的参数名映射及权限码。
 * paramByName 含按参数名（需 -parameters）及 #p0/#a0 位置别名，供资源解析器读取方法参数。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record AbacMethodContext(
        Method method,
        Class<?> targetClass,
        Object[] args,
        Map<String, Object> paramByName,
        String permission
) {
    public AbacMethodContext {
        args = args.clone();
        paramByName = Map.copyOf(paramByName);
    }
}