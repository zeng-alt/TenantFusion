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
 * 配置表单主表。
 * <p>
 * 记录一张通过结构化字段配置（非 FormKit 拖拽设计）构建的动态表单主数据。
 * 每条表单由多个字段（{@link FormFieldEntity}）组成，支持 List/Map/Object 嵌套、
 * 校验规则、条件渲染等高级特性。
 * 版本管理沿用 master + version child 模式，字段数据挂在版本上。
 *
 * @author zengAlt
 */
@Entity
@Table(name = "wf_form_config")
@Getter
@Setter
public class FormConfigEntity extends BaseEntity<Long> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @SnowflakeId
    @Column(name = "form_config_id")
    private Long formConfigId;

    /** 表单名称 */
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    /** 表单编码（全局唯一） */
    @Column(name = "code", nullable = false, unique = true, length = 128)
    private String code;

    /** 表单分类 */
    @Column(name = "category", length = 64)
    private String category;

    /** 当前生效的已发布版本号（0 表示从未发布） */
    @Column(name = "current_version")
    private Integer currentVersion = 0;

    /** 最新版本号（含草稿） */
    @Column(name = "latest_version")
    private Integer latestVersion = 0;

    /** 表单描述 */
    @Column(name = "description", length = 512)
    private String description;

    /** 备注 */
    @Column(name = "remark", length = 512)
    private String remark;

    @Override
    public Long getId() {
        return formConfigId;
    }
}
