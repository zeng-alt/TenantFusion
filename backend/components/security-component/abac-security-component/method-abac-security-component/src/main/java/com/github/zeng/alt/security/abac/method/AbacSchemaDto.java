package com.github.zeng.alt.security.abac.method;

import java.util.List;

/**
 * 单个方法权限可用的表达式上下文。
 *
 * @param permission 权限码
 * @param globals 公共及可信资源字段
 * @param methods 方法绑定
 * @param functions 可调用的授权函数
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record AbacSchemaDto(
    String permission,
    List<AbacFieldDto> globals,
    List<AbacMethodSchemaDto> methods,
    List<String> functions
) {
    public AbacSchemaDto {
        globals = List.copyOf(globals);
        methods = List.copyOf(methods);
        functions = List.copyOf(functions);
    }
}
