package com.github.zeng.alt.workflow.model;

/**
 * 任务节点表单定义类型
 *
 * @author zengAlt
 */
public enum TaskFormType {

    /** FormKit 动态表单模板（camunda:formRef 引用模板编码） */
    FORM_TEMPLATE,

    /** 前端资源表单（camunda:formKey，如 test.vue） */
    FORM_KEY,

    /** 内置表单（camunda:formData 内联字段） */
    FORM_DATA
}
