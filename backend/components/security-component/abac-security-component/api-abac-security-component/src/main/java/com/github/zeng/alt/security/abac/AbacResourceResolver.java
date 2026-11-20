package com.github.zeng.alt.security.abac;

import java.util.Map;

/**
 * 资源解析 SPI（宿主实现并注册为 Spring bean，在 {@link AbacAuthorize#resources()} 显式声明）。
 * 从方法上下文贡献标量资源属性，多个解析器结果按声明顺序合并（后者覆盖同名键）。
 * 属性由服务端可信数据源解析，供表达式读取。不得返回 null。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public interface AbacResourceResolver {
    Map<String, Object> resolve(AbacMethodContext context);
    /** 提供编辑器可用的可信资源字段；实现可声明标量字段类型。 */
    default Map<String, Class<?>> attributes() { return Map.of(); }
}
