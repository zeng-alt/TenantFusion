package com.github.zeng.alt.workflow.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * FormDefinition JSON 字符串 → JsonNode 的转换器。
 * <p>
 * 供 MapStruct（FormTemplateVersionMapper）通过 {@code uses} 注入调用；
 * 解析失败时返回 null，不影响版本主数据展示。
 *
 * @author zengAlt
 */
@Component
public class DefinitionJsonMapper {

    private final ObjectMapper objectMapper;

    public DefinitionJsonMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 解析表单定义 JSON 字符串
     *
     * @param definition 定义 JSON 字符串
     * @return 定义对象，空/非法 JSON 返回 null
     */
    public JsonNode parse(String definition) {
        if (definition == null || definition.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(definition);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}