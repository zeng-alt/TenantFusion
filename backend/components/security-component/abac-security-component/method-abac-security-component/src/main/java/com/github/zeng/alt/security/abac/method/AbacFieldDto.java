package com.github.zeng.alt.security.abac.method;

import java.util.List;

/**
 * 编辑器使用的字段描述，不包含真实请求值。
 *
 * @param path 表达式路径
 * @param type Java 类型名称
 * @param nullable 是否可空
 * @param source 字段来源
 * @param fields 子字段
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record AbacFieldDto(String path, String type, boolean nullable, String source, List<AbacFieldDto> fields) {
    public AbacFieldDto { fields = List.copyOf(fields); }
}
