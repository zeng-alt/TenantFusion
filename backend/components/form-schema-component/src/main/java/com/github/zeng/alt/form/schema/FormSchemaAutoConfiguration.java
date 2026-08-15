package com.github.zeng.alt.form.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 表单结构组件自动配置：
 * 注册 DSL 解析 / 表达式求值 / 数据校验 / 结构转换相关 Bean。
 *
 * @author zengAlt
 */
@AutoConfiguration
public class FormSchemaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DslParser dslParser() {
        return new DslParser();
    }

    @Bean
    @ConditionalOnMissingBean
    public DslExprEvaluator dslExprEvaluator() {
        return new DslExprEvaluator();
    }

    @Bean
    @ConditionalOnMissingBean
    public DslDataValidator dslDataValidator(DslParser dslParser, DslExprEvaluator dslExprEvaluator) {
        return new DslDataValidator(dslParser, dslExprEvaluator);
    }

    @Bean
    @ConditionalOnMissingBean
    public DslFormSchemaConverter dslFormSchemaConverter(ObjectMapper objectMapper) {
        return new DslFormSchemaConverter(objectMapper);
    }
}
