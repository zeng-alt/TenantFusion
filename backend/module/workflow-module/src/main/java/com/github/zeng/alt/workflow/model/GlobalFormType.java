package com.github.zeng.alt.workflow.model;

/**
 * 全局表单定义类型
 *
 * @author zengAlt
 */
public enum GlobalFormType {

    /** Camunda 动态表单模板（globalForm.formRef 引用模板编码，绑定方式由 formRefBinding 决定） */
    CAMUNDA,

    /** 外部前端资源表单（globalForm.formKey 为前端 vue 地址） */
    EXTERNAL,

    /** 内联生成的表单（globalForm.fields 为表单定义数据） */
    GENERATED
}
