package com.github.zeng.alt.workflow.entity;

import com.github.zeng.alt.domain.base.BaseEntity;
import com.github.zeng.alt.domain.key.SnowflakeId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * 动态表单模板主表。
 * <p>
 * 记录一张可由 FormKit 表单设计器（formkit-form-builder）可视化设计的表单模板主数据。
 * 渲染结构（FormDefinition JSON）保存在版本表（{@link FormTemplateVersionEntity}）中，
 * 支持草稿、发布、下线等状态流转与版本管理。
 *
 * @author zengAlt
 */
@Entity
@Table(name = "wf_form_template")
@Getter
@Setter
public class FormTemplateEntity extends BaseEntity<Long> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @SnowflakeId
    @Column(name = "form_template_id")
    private Long formTemplateId;

    /** 模板名称 */
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    /** 模板编码（全局唯一） */
    @Column(name = "code", nullable = false, unique = true, length = 128)
    private String code;

    /** 模板分类（如：人事 / 财务 / 行政） */
    @Column(name = "category", length = 64)
    private String category;

    /** 当前生效的已发布版本号（0 表示从未发布） */
    @Column(name = "current_version")
    private Integer currentVersion = 0;

    /** 最新版本号（含草稿） */
    @Column(name = "latest_version")
    private Integer latestVersion = 0;

    /** 模板描述 */
    @Column(name = "description", length = 512)
    private String description;

    /** 备注 */
    @Column(name = "remark", length = 512)
    private String remark;

    @Override
    public Long getId() {
        return formTemplateId;
    }
}