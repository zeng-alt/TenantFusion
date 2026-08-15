package com.github.zeng.alt.form.schema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DslDataValidator} 单元测试：校验规则 + visibleIf 条件显示跳过隐藏字段。
 *
 * @author zengAlt
 */
class DslDataValidatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private DslDataValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DslDataValidator(new DslParser(), new DslExprEvaluator());
    }

    private JsonNode definition() throws JsonProcessingException {
        return objectMapper.readTree("""
                {
                  "version": 1,
                  "id": "t1",
                  "name": "测试",
                  "root": {
                    "id": "root",
                    "type": "group",
                    "category": "container",
                    "renderAs": "formkit",
                    "dataType": "object",
                    "children": [
                      { "id": "f1", "type": "text", "category": "field", "name": "name",
                        "validation": [ { "rule": "required", "message": "姓名必填" } ] },
                      { "id": "f2", "type": "number", "category": "field", "name": "age",
                        "validation": [ { "rule": "number" }, { "rule": "min", "args": [18] } ] },
                      { "id": "f3", "type": "text", "category": "field", "name": "phone",
                        "validation": [ { "rule": "pattern", "args": ["^1\\\\d{10}$"], "message": "手机号格式不正确" } ] },
                      { "id": "f4", "type": "select", "category": "field", "name": "gender",
                        "validation": [ { "rule": "one_of", "args": ["M", "F"] } ] },
                      { "id": "f5", "type": "text", "category": "field", "name": "street",
                        "visibleIf": { "type": "call", "fn": "eq",
                          "args": [ { "type": "field", "name": "gender" }, { "type": "literal", "value": "F" } ] },
                        "validation": [ { "rule": "required", "message": "街道必填" } ] }
                    ]
                  },
                  "settings": { "layout": "vertical" }
                }
                """);
    }

    @Test
    void validDataNoErrors() throws JsonProcessingException {
        Map<String, Object> data = Map.of(
                "name", "张三",
                "age", 20,
                "phone", "13800138000",
                "gender", "M");
        assertThat(validator.validate(definition(), data)).isEmpty();
    }

    @Test
    void requiredMessageWins() throws JsonProcessingException {
        Map<String, Object> data = Map.of("age", 20, "gender", "M");
        Map<String, String> errors = validator.validate(definition(), data);
        assertThat(errors).containsEntry("name", "姓名必填");
    }

    @Test
    void minRuleRejectsTooSmall() throws JsonProcessingException {
        Map<String, Object> data = Map.of("name", "张三", "age", 16, "gender", "M");
        Map<String, String> errors = validator.validate(definition(), data);
        assertThat(errors).containsEntry("age", "不能小于 18");
    }

    @Test
    void numberRuleRejectsNonNumeric() throws JsonProcessingException {
        Map<String, Object> data = Map.of("name", "张三", "age", "abc", "gender", "M");
        Map<String, String> errors = validator.validate(definition(), data);
        assertThat(errors).containsEntry("age", "请输入数字");
    }

    @Test
    void patternRuleCustomMessage() throws JsonProcessingException {
        Map<String, Object> data = Map.of("name", "张三", "age", 20, "phone", "123", "gender", "M");
        Map<String, String> errors = validator.validate(definition(), data);
        assertThat(errors).containsEntry("phone", "手机号格式不正确");
    }

    @Test
    void oneOfRuleRejectsUnknownValue() throws JsonProcessingException {
        Map<String, Object> data = Map.of("name", "张三", "age", 20, "gender", "X");
        Map<String, String> errors = validator.validate(definition(), data);
        assertThat(errors).containsEntry("gender", "不在允许的选项范围内");
    }

    @Test
    void visibleIfHidesFieldWhenFalse() throws JsonProcessingException {
        Map<String, Object> data = Map.of("name", "张三", "age", 20, "gender", "M");
        assertThat(validator.validate(definition(), data)).doesNotContainKeys("street");
    }

    @Test
    void visibleIfExposesFieldWhenTrue() throws JsonProcessingException {
        Map<String, Object> data = Map.of("name", "张三", "age", 20, "gender", "F");
        Map<String, String> errors = validator.validate(definition(), data);
        assertThat(errors).containsEntry("street", "街道必填");
    }
}
