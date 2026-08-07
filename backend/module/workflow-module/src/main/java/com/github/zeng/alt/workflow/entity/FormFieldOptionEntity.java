package com.github.zeng.alt.workflow.entity;

import com.github.zeng.alt.domain.base.BaseEntity;
import com.github.zeng.alt.domain.key.SnowflakeId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * 配置表单字段选项表（用于 SELECT / MULTI_SELECT 类型字段的下拉选项）
 *
 * @author zengAlt
 */
@Entity
@Table(name = "wf_form_field_option")
@Getter
@Setter
public class FormFieldOptionEntity extends BaseEntity<Long> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @SnowflakeId
    @Column(name = "option_id")
    private Long optionId;

    /** 所属字段ID */
    @Column(name = "field_id", nullable = false)
    private Long fieldId;

    /** 选项标签 */
    @Column(name = "label", nullable = false, length = 128)
    private String label;

    /** 选项值 */
    @Column(name = "option_value", nullable = false, length = 256)
    private String value;

    /** 排序号 */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Override
    public Long getId() {
        return optionId;
    }
}
