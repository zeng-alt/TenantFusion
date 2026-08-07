package com.github.zeng.alt.workflow.entity;

import com.github.zeng.alt.domain.base.BaseEntity;
import com.github.zeng.alt.domain.key.SnowflakeId;
import com.github.zeng.alt.workflow.model.FieldType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * 配置表单字段表。
 * <p>
 * 每个字段属于一个表单配置版本（{@link FormFieldOptionEntity}），
 * 支持通过 {@link #parentFieldId} 实现 List/Map/Object 类型的嵌套子字段。
 * 校验规则、条件渲染、类型特定属性均以 JSON 结构化存储。
 *
 * @author zengAlt
 */
@Entity
@Table(name = "wf_form_field")
@Getter
@Setter
public class FormFieldEntity extends BaseEntity<Long> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @SnowflakeId
    @Column(name = "field_id")
    private Long fieldId;

    /** 所属版本ID */
    @Column(name = "version_id", nullable = false)
    private Long versionId;

    /** 父字段ID（用于 List/Map/Object 的嵌套子字段，顶层字段为 null） */
    @Column(name = "parent_field_id")
    private Long parentFieldId;

    /** 字段标识（camelCase，用于数据绑定） */
    @Column(name = "field_key", nullable = false, length = 64)
    private String fieldKey;

    /** 字段标签（显示名称） */
    @Column(name = "field_label", nullable = false, length = 128)
    private String fieldLabel;

    /** 字段类型 */
    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false, length = 20)
    private FieldType fieldType;

    /** 默认值（JSON 字符串，可存储任意类型默认值） */
    @Column(name = "default_value", length = 1024)
    private String defaultValue;

    /** 占位提示 */
    @Column(name = "placeholder", length = 256)
    private String placeholder;

    /** 帮助文本 */
    @Column(name = "help_text", length = 512)
    private String helpText;

    /** 排序号 */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    /** 栅格列宽（1-24） */
    @Column(name = "col_span")
    private Integer colSpan = 24;

    /** 是否必填 */
    @Column(name = "is_required")
    private Boolean required = false;

    /** 是否只读 */
    @Column(name = "is_readonly")
    private Boolean readonly = false;

    /** 是否隐藏 */
    @Column(name = "is_hidden")
    private Boolean hidden = false;

    /** 校验规则（JSON 数组，如 [{"type":"min","value":1,"message":"不能小于1"}]） */
    @Lob
    @Column(name = "validation_rules", columnDefinition = "CLOB")
    private String validationRules;

    /** 条件渲染规则（JSON，如 {"fieldKey":"type","operator":"eq","value":"other"}） */
    @Lob
    @Column(name = "visibility_condition", columnDefinition = "CLOB")
    private String visibilityCondition;

    /** 类型特定属性（JSON，如 {"min":0,"max":100,"step":1,"dateFormat":"yyyy-MM-dd","fileTypes":["jpg","png"]}） */
    @Lob
    @Column(name = "field_props", columnDefinition = "CLOB")
    private String fieldProps;

    @Override
    public Long getId() {
        return fieldId;
    }
}
