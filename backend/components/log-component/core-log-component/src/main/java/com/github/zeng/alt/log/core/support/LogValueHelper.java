package com.github.zeng.alt.log.core.support;

import com.github.zeng.alt.log.core.config.LogProperties;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * 请求和响应共用的 Jackson 3 日志字段过滤。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public final class LogValueHelper {
    private final JsonMapper mapper;
    private final LogProperties properties;

    /** 先应用 Jackson 业务注解，再递归排除对象和数组中的敏感字段。 */
    public String toJson(Object value, String[] excludedFields) {
        final Set<String> excluded = Stream.concat(properties.getExcludedFields().stream(), Arrays.stream(excludedFields))
                .map(name -> name.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
        final JsonNode tree = mapper.valueToTree(value);
        exclude(tree, excluded);
        return mapper.writeValueAsString(tree);
    }

    private void exclude(JsonNode node, Set<String> excluded) {
        if (node instanceof ObjectNode object) {
            object.properties().stream().map(java.util.Map.Entry::getKey)
                    .filter(name -> excluded.contains(name.toLowerCase(Locale.ROOT)))
                    .toList().forEach(object::remove);
        }
        node.forEach(child -> exclude(child, excluded));
    }
}
