package com.github.zeng.alt.workflow.model;

/**
 * 表单字段类型枚举
 *
 * @author zengAlt
 */
public enum FieldType {

    /** 单行文本 */
    STRING,

    /** 多行文本 */
    TEXTAREA,

    /** 数字 */
    NUMBER,

    /** 布尔开关 */
    BOOLEAN,

    /** 日期（yyyy-MM-dd） */
    DATE,

    /** 日期时间（yyyy-MM-dd HH:mm:ss） */
    DATETIME,

    /** 下拉单选 */
    SELECT,

    /** 下拉多选 */
    MULTI_SELECT,

    /** 文件上传 */
    FILE,

    /** 图片上传 */
    IMAGE,

    /** 富文本 */
    RICH_TEXT,

    /** 列表（可嵌套子字段，渲染为动态表格/列表） */
    LIST,

    /** 键值映射（可嵌套子字段） */
    MAP,

    /** 对象（可嵌套子字段，渲染为字段组） */
    OBJECT
}
