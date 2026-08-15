package com.github.zeng.alt.form.schema;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * FormDefinition JSON → {@link DslNode} 树的解析器（纯 Jackson，无 json-component 依赖）。
 * <p>
 * 只抽取服务端校验所需字段，未知节点 / 属性一律透传忽略。
 *
 * @author zengAlt
 */
public class DslParser {

    /**
     * 解析表单定义根节点
     *
     * @param definition FormDefinition JSON
     * @return 根容器节点（root 缺失或非法时返回 null）
     */
    public DslNode parse(JsonNode definition) {
        if (definition == null || !definition.isObject()) {
            return null;
        }
        return parseNode(definition.path("root"));
    }

    private DslNode parseNode(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }
        List<DslNode> children = new ArrayList<>();
        JsonNode childrenNode = node.path("children");
        if (childrenNode.isArray()) {
            for (JsonNode child : childrenNode) {
                DslNode parsed = parseNode(child);
                if (parsed != null) {
                    children.add(parsed);
                }
            }
        }
        return new DslNode(
                text(node, "id"),
                text(node, "type"),
                text(node, "category"),
                text(node, "name"),
                text(node, "label"),
                node.get("visibleIf"),
                parseValidation(node.path("validation")),
                children
        );
    }

    private List<DslValidationRule> parseValidation(JsonNode validation) {
        if (!validation.isArray()) {
            return List.of();
        }
        List<DslValidationRule> rules = new ArrayList<>();
        for (JsonNode ruleNode : validation) {
            if (!ruleNode.isObject()) {
                continue;
            }
            List<Object> args = new ArrayList<>();
            JsonNode argsNode = ruleNode.path("args");
            if (argsNode.isArray()) {
                for (JsonNode arg : argsNode) {
                    args.add(toJavaValue(arg));
                }
            }
            rules.add(new DslValidationRule(
                    ruleNode.path("rule").asText(),
                    args,
                    text(ruleNode, "message"),
                    intOrNull(ruleNode, "debounce"),
                    boolOrNull(ruleNode, "empty"),
                    boolOrNull(ruleNode, "force"),
                    boolOrNull(ruleNode, "optional")
            ));
        }
        return rules;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() || value.isMissingNode() ? null : value.asText();
    }

    private static Integer intOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isMissingNode() || !value.isNumber() ? null : value.asInt();
    }

    private static Boolean boolOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isMissingNode() || !value.isBoolean() ? null : value.asBoolean();
    }

    private static Object toJavaValue(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isNumber()) {
            return node.isIntegralNumber() ? node.longValue() : node.decimalValue();
        }
        if (node.isArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonNode item : node) {
                list.add(toJavaValue(item));
            }
            return list;
        }
        if (node.isObject()) {
            java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<>();
            node.fields().forEachRemaining(entry -> map.put(entry.getKey(), toJavaValue(entry.getValue())));
            return map;
        }
        return node.asText();
    }
}
