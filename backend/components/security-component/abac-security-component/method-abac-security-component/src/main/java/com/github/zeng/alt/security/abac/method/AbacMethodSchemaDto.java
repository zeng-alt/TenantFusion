package com.github.zeng.alt.security.abac.method;

import java.util.List;

/**
 * 权限绑定方法的输入输出类型。
 *
 * @param method 方法名称
 * @param transactional 是否声明事务
 * @param parameters 参数及位置别名
 * @param returned 成功返回值的类型
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record AbacMethodSchemaDto(String method, boolean transactional,
        List<AbacParameterDto> parameters, AbacFieldDto returned) {
    public AbacMethodSchemaDto { parameters = List.copyOf(parameters); }
}
