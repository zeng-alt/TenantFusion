package com.github.zeng.alt.form.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

/**
 * FormDefinition（DSL）→ 表单数据结构（FormSchema）转换器。
 * <p>
 * 输出与前端约定的 {@code FormSchemaField[]} 结构一致：
 * <pre>
 *   type: 'string' | 'long' | 'double' | 'boolean' | 'date' | 'enum' | 'object' | 'array'
 * </pre>
 * 映射规则：
 * <ul>
 *   <li>字段节点按 {@code type} 映射为标量 / 枚举 / 日期；选项来自 {@code options}（静态数组或动态字典 {@code {dynamic,code}}）</li>
 *   <li>布局节点（grid/row/column/card/tabs/steps 等）不产出数据 key，子节点扁平化到当前层级</li>
 *   <li>{@code object} 容器（group/inputGroup）→ type=object + children</li>
 *   <li>{@code array} 容器（list）→ type=array + items（单字段为标量，否则包成 object）</li>
 *   <li>纯展示壳（buttonGroup/badge/dataTable）与静态节点（文本/标题/分割线等）跳过</li>
 * </ul>
 * <p>
 * 纯 Jackson 实现、无 Spring 依赖，由 {@link FormSchemaAutoConfiguration} 注册为 Bean。
 *
 * @author zengAlt
 */
public class DslFormSchemaConverter {

    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    private static final String DEFAULT_TIME_FORMAT = "HH:mm:ss";
    private static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private final ObjectMapper objectMapper;

    public DslFormSchemaConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 转换表单定义为 FormSchema（顶层字段数组）
     *
     * @param definition FormDefinition JSON（root 缺失时返回空数组）
     * @return FormSchemaField[] 数组
     */
    public JsonNode convert(JsonNode definition) {
        ArrayNode result = objectMapper.createArrayNode();
        if (definition == null || !definition.isObject()) {
            return result;
        }
        JsonNode root = definition.path("root");
        if (root.isObject()) {
            List<JsonNode> fields = new ArrayList<>();
            convertChildren(root, fields);
            fields.forEach(result::add);
        }
        return result;
    }

    private void convertChildren(JsonNode node, List<JsonNode> out) {
        for (JsonNode child : node.path("children")) {
            if (!child.isObject()) {
                continue;
            }
            String category = child.path("category").asText();
            switch (category) {
                case "field" -> out.add(fieldNode(child));
                case "container" -> convertContainer(child, out);
                case "layout" -> convertChildren(child, out);
                default -> {
                    // static：无数据 key，跳过
                }
            }
        }
    }

    private void convertContainer(JsonNode node, List<JsonNode> out) {
        String dataType = dataTypeOf(node);
        if ("array".equals(dataType)) {
            out.add(arrayNode(node));
        } else if ("object".equals(dataType)) {
            JsonNode object = objectNode(node);
            if (object != null) {
                out.add(object);
            }
        } else {
            // none（buttonGroup/badge/dataTable）：纯展示壳，子节点透传（一般无）
            convertChildren(node, out);
        }
    }

    /** 容器数据结构：缺省按类型推断（list → array，其余 object） */
    private String dataTypeOf(JsonNode node) {
        JsonNode dataType = node.get("dataType");
        if (dataType != null && !dataType.isNull() && !dataType.asText().isBlank()) {
            return dataType.asText();
        }
        return "list".equals(node.path("type").asText()) ? "array" : "object";
    }

    private JsonNode objectNode(JsonNode node) {
        String name = node.path("name").asText();
        List<JsonNode> children = new ArrayList<>();
        convertChildren(node, children);
        if (name.isBlank() && children.isEmpty()) {
            return null;
        }
        ObjectNode object = baseField(node, "object");
        if (!children.isEmpty()) {
            ArrayNode childArray = objectMapper.createArrayNode();
            children.forEach(childArray::add);
            object.set("children", childArray);
        }
        return object;
    }

    private JsonNode arrayNode(JsonNode node) {
        ObjectNode array = baseField(node, "array");
        List<JsonNode> recordFields = new ArrayList<>();
        convertChildren(node, recordFields);
        // 解包匿名 object 记录（list 内常见的未命名 group）：记录字段即其子字段
        boolean objectRecord = recordFields.size() == 1
                && "object".equals(recordFields.get(0).path("type").asText())
                && recordFields.get(0).path("name").asText().isBlank();
        if (objectRecord) {
            List<JsonNode> unwrapped = new ArrayList<>();
            recordFields.get(0).path("children").forEach(unwrapped::add);
            recordFields = unwrapped;
        }
        if (!objectRecord && recordFields.size() == 1 && isScalar(recordFields.get(0))) {
            // 真正的标量数组（如数字列表）
            array.set("items", recordFields.get(0));
        } else {
            // 对象数组：记录为 object，字段是其 children
            ObjectNode items = objectMapper.createObjectNode();
            items.put("type", "object");
            if (!recordFields.isEmpty()) {
                ArrayNode itemChildren = objectMapper.createArrayNode();
                recordFields.forEach(itemChildren::add);
                items.set("children", itemChildren);
            }
            array.set("items", items);
        }
        return array;
    }

    private JsonNode fieldNode(JsonNode node) {
        String type = node.path("type").asText();
        String name = node.path("name").asText();
        if (name.isBlank()) {
            return null;
        }
        JsonNode options = resolveOptions(node);
        ObjectNode field = baseField(node, stringType(type, options));
        switch (type) {
            case "number" -> {
                JsonNode numberProp = node.path("props").get("number");
                field.put("type", numberProp != null && "integer".equals(numberProp.asText()) ? "long" : "double");
            }
            case "range" -> field.put("type", "double");
            case "naiveRate" -> field.put("type", "long");
            case "date", "time", "naiveDateTime" -> {
                field.put("type", "date");
                field.put("datePattern", datePatternOf(type, node));
            }
            case "select", "radio", "checkbox", "naiveCascader", "naiveTreeSelect", "naiveTransfer" -> {
                if (!hasOptions(options)) {
                    field.put("type", "boolean");
                    break;
                }
                field.put("type", "enum");
                appendOptions(field, options);
            }
            case "naiveSwitch" -> field.put("type", "boolean");
            default -> field.put("type", "string");
        }
        return field;
    }

    /** 构造基础字段：name + label（缺省 name）+ 类型占位 */
    private ObjectNode baseField(JsonNode node, String type) {
        ObjectNode field = objectMapper.createObjectNode();
        field.put("name", node.path("name").asText());
        String label = node.path("label").asText();
        if (label != null && !label.isBlank()) {
            field.put("label", label);
        }
        field.put("type", type);
        return field;
    }

    /**
     * 标量类型缺省映射：枚举型字段无选项时回落 string（checkbox 回落 boolean）
     */
    private String stringType(String type, JsonNode options) {
        return switch (type) {
            case "naiveSwitch" -> "boolean";
            case "checkbox" -> hasOptions(options) ? "enum" : "boolean";
            case "select", "radio", "naiveCascader", "naiveTreeSelect", "naiveTransfer" ->
                    hasOptions(options) ? "enum" : "string";
            default -> "string";
        };
    }

    private boolean hasOptions(JsonNode options) {
        if (options == null) {
            return false;
        }
        if (options.isArray()) {
            return options.size() > 0;
        }
        return options.isObject() && options.path("dynamic").asBoolean(false);
    }

    /** 字段选项：优先节点 options（静态数组 / 动态字典），回落 props.options */
    private JsonNode resolveOptions(JsonNode node) {
        JsonNode options = node.get("options");
        if (options == null || options.isNull()) {
            options = node.path("props").get("options");
        }
        return options == null || options.isNull() ? null : options;
    }

    private String datePatternOf(String type, JsonNode node) {
        JsonNode valueFormat = node.path("props").get("valueFormat");
        if (valueFormat != null && !valueFormat.asText().isBlank()) {
            return valueFormat.asText();
        }
        return switch (type) {
            case "time" -> DEFAULT_TIME_FORMAT;
            case "naiveDateTime" -> DEFAULT_DATE_TIME_FORMAT;
            default -> DEFAULT_DATE_FORMAT;
        };
    }

    /**
     * 写入 options（FormSchemaEnumOptions）与 enumValues：
     * 静态字符串数组 / {label,value,disabled} 对象数组 / 动态字典 {dynamic,code,label}
     */
    private void appendOptions(ObjectNode field, JsonNode options) {
        if (options.isObject() && options.path("dynamic").asBoolean(false)) {
            ObjectNode dynamic = objectMapper.createObjectNode();
            dynamic.put("dynamic", true);
            dynamic.put("code", options.path("code").asText());
            if (options.has("label")) {
                dynamic.put("label", options.path("label").asText());
            }
            field.set("options", dynamic);
            return;
        }
        ArrayNode optionArray = objectMapper.createArrayNode();
        ArrayNode enumValues = objectMapper.createArrayNode();
        for (JsonNode option : options) {
            if (option.isTextual()) {
                optionArray.add(option.asText());
                enumValues.add(enumValue(option.asText(), option.asText()));
            } else {
                String label = firstText(option, "label", "name");
                String value = firstText(option, "value", "key", "id");
                if (label.isBlank() && value.isBlank()) {
                    continue;
                }
                ObjectNode item = objectMapper.createObjectNode();
                item.put("label", label);
                item.put("value", value);
                if (option.path("disabled").asBoolean(false)) {
                    item.put("disabled", true);
                }
                optionArray.add(item);
                enumValues.add(enumValue(value, label));
            }
        }
        if (optionArray.size() > 0) {
            field.set("options", optionArray);
        }
        if (enumValues.size() > 0) {
            field.set("enumValues", enumValues);
        }
    }

    private ObjectNode enumValue(String id, String name) {
        ObjectNode value = objectMapper.createObjectNode();
        value.put("id", id);
        value.put("name", name);
        return value;
    }

    private static String firstText(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && !value.isNull() && !value.isMissingNode()) {
                return value.asText();
            }
        }
        return "";
    }

    private static boolean isScalar(JsonNode field) {
        String type = field.path("type").asText();
        return !"object".equals(type) && !"array".equals(type);
    }
}
