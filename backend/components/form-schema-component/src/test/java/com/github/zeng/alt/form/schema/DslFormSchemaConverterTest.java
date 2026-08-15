package com.github.zeng.alt.form.schema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DslFormSchemaConverter} 单元测试：DSL → FormSchemaField[] 映射。
 *
 * @author zengAlt
 */
class DslFormSchemaConverterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private DslFormSchemaConverter converter;

    @BeforeEach
    void setUp() {
        converter = new DslFormSchemaConverter(objectMapper);
    }

    private JsonNode convert(String definition) throws JsonProcessingException {
        return converter.convert(objectMapper.readTree(definition));
    }

    @Test
    void mapsScalarFieldTypes() throws JsonProcessingException {
        JsonNode schema = convert("""
                {
                  "root": { "category": "container", "type": "group", "dataType": "object", "children": [
                    { "id": "1", "category": "field", "type": "text", "name": "title", "label": "标题" },
                    { "id": "2", "category": "field", "type": "number", "name": "age", "props": { "number": "integer" } },
                    { "id": "3", "category": "field", "type": "number", "name": "amount" },
                    { "id": "4", "category": "field", "type": "naiveSwitch", "name": "enabled" },
                    { "id": "5", "category": "field", "type": "naiveRate", "name": "level" }
                  ] }
                }
                """);
        assertThat(schema.get(0).path("type").asText()).isEqualTo("string");
        assertThat(schema.get(0).path("label").asText()).isEqualTo("标题");
        assertThat(schema.get(1).path("type").asText()).isEqualTo("long");
        assertThat(schema.get(2).path("type").asText()).isEqualTo("double");
        assertThat(schema.get(3).path("type").asText()).isEqualTo("boolean");
        assertThat(schema.get(4).path("type").asText()).isEqualTo("long");
    }

    @Test
    void mapsDateWithPattern() throws JsonProcessingException {
        JsonNode schema = convert("""
                {
                  "root": { "category": "container", "type": "group", "dataType": "object", "children": [
                    { "id": "1", "category": "field", "type": "date", "name": "birthday" },
                    { "id": "2", "category": "field", "type": "naiveDateTime", "name": "startAt",
                      "props": { "valueFormat": "yyyy-MM-dd HH:mm:ss" } }
                  ] }
                }
                """);
        assertThat(schema.get(0).path("type").asText()).isEqualTo("date");
        assertThat(schema.get(0).path("datePattern").asText()).isEqualTo("yyyy-MM-dd");
        assertThat(schema.get(1).path("datePattern").asText()).isEqualTo("yyyy-MM-dd HH:mm:ss");
    }

    @Test
    void mapsEnumStaticAndDynamicOptions() throws JsonProcessingException {
        JsonNode schema = convert("""
                {
                  "root": { "category": "container", "type": "group", "dataType": "object", "children": [
                    { "id": "1", "category": "field", "type": "select", "name": "city",
                      "options": [ { "label": "北京", "value": "bj" }, { "label": "上海", "value": "sh", "disabled": true } ] },
                    { "id": "2", "category": "field", "type": "radio", "name": "dept",
                      "options": { "dynamic": true, "code": "dept_dict", "label": "部门" } }
                  ] }
                }
                """);
        JsonNode city = schema.get(0);
        assertThat(city.path("type").asText()).isEqualTo("enum");
        assertThat(city.path("options").isArray()).isTrue();
        assertThat(city.path("options").get(1).path("disabled").asBoolean()).isTrue();
        assertThat(city.path("enumValues").get(0).path("id").asText()).isEqualTo("bj");
        JsonNode dept = schema.get(1);
        assertThat(dept.path("type").asText()).isEqualTo("enum");
        assertThat(dept.path("options").path("dynamic").asBoolean()).isTrue();
        assertThat(dept.path("options").path("code").asText()).isEqualTo("dept_dict");
    }

    @Test
    void mapsObjectAndArrayContainers() throws JsonProcessingException {
        JsonNode schema = convert("""
                {
                  "root": { "category": "container", "type": "group", "dataType": "object", "children": [
                    { "id": "1", "category": "container", "type": "group", "name": "user",
                      "children": [ { "id": "1-1", "category": "field", "type": "text", "name": "nickname" } ] },
                    { "id": "2", "category": "container", "type": "list", "name": "items",
                      "children": [ { "id": "2-1", "category": "container", "type": "group",
                        "children": [ { "id": "2-1-1", "category": "field", "type": "text", "name": "name" } ] } ] }
                  ] }
                }
                """);
        JsonNode user = schema.get(0);
        assertThat(user.path("type").asText()).isEqualTo("object");
        assertThat(user.path("children").get(0).path("name").asText()).isEqualTo("nickname");
        JsonNode items = schema.get(1);
        assertThat(items.path("type").asText()).isEqualTo("array");
        assertThat(items.path("items").path("type").asText()).isEqualTo("object");
        assertThat(items.path("items").path("children").get(0).path("name").asText()).isEqualTo("name");
    }

    @Test
    void flattensLayoutAndSkipsStatic() throws JsonProcessingException {
        JsonNode schema = convert("""
                {
                  "root": { "category": "container", "type": "group", "dataType": "object", "children": [
                    { "id": "1", "category": "layout", "type": "card", "children": [
                      { "id": "1-1", "category": "field", "type": "text", "name": "inside" }
                    ] },
                    { "id": "2", "category": "static", "type": "text", "text": "说明文字" }
                  ] }
                }
                """);
        assertThat(schema.size()).isEqualTo(1);
        assertThat(schema.get(0).path("name").asText()).isEqualTo("inside");
    }
}
