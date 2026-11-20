package com.github.zeng.alt.security.abac.method;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 装配公共上下文的字段定义，贡献字段优先于组件默认字段。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class AbacSchemaBuilder {
    private final Map<String, Map<String, Class<?>>> roots = new LinkedHashMap<>();

    public AbacSchemaBuilder field(String root, String name, Class<?> type) {
        roots.computeIfAbsent(root, ignored -> new LinkedHashMap<>()).putIfAbsent(name, type);
        return this;
    }

    public List<AbacFieldDto> build() {
        final List<AbacFieldDto> fields = new ArrayList<>();
        roots.forEach((name, children) -> fields.add(new AbacFieldDto(name, "Object", false, "服务端快照",
                children.entrySet().stream().map(entry -> new AbacFieldDto(name + "." + entry.getKey(),
                        entry.getValue().getSimpleName(), !entry.getValue().isPrimitive(), "服务端快照", List.of())).toList())));
        return List.copyOf(fields);
    }
}
