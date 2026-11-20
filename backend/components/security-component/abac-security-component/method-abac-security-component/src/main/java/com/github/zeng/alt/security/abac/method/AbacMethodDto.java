package com.github.zeng.alt.security.abac.method;

import org.springframework.core.ResolvableType;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * 方法的进程内静态元数据，不包含实参和业务返回值，也不参与网络序列化。
 *
 * @param method 解析泛型桥接后的具体方法
 * @param targetClass 目标用户类型
 * @param argumentIndices 参数名与 pN/aN 别名对应的位置
 * @param parameterTypes 在目标类型中解析后的泛型参数类型
 * @param returnType 在目标类型中解析后的泛型返回类型
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record AbacMethodDto(
    Method method,
    Class<?> targetClass,
    Map<String, Integer> argumentIndices,
    List<ResolvableType> parameterTypes,
    ResolvableType returnType
) {
    public AbacMethodDto {
        argumentIndices = Map.copyOf(argumentIndices);
        parameterTypes = List.copyOf(parameterTypes);
    }
}
