package com.github.zeng.alt.workflow.entity;

import com.github.zeng.alt.domain.base.BaseEntity;
import com.github.zeng.alt.domain.key.SnowflakeId;
import com.github.zeng.alt.workflow.model.FormConfigVersionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 配置表单版本表。
 * <p>
 * 每个配置表单（{@link FormConfigEntity}）对应多条版本记录。
 * 版本状态支持：DRAFT → PUBLISHED → OFFLINE。
 * 字段数据通过 {@link FormFieldEntity#versionId} 关联版本。
 *
 * @author zengAlt
 */
@Entity
@Table(name = "wf_form_config_version")
@Getter
@Setter
public class FormConfigVersionEntity extends BaseEntity<Long> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @SnowflakeId
    @Column(name = "version_id")
    private Long versionId;

    /** 所属配置表单ID */
    @Column(name = "form_config_id", nullable = false)
    private Long formConfigId;

    /** 版本号（同一表单内从 1 递增） */
    @Column(name = "version", nullable = false)
    private Integer version;

    /** 版本状态 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FormConfigVersionStatus status = FormConfigVersionStatus.DRAFT;

    /** 是否当前生效版本 */
    @Column(name = "is_current")
    private Boolean current = false;

    /** 发布时间 */
    @Column(name = "published_date")
    private LocalDateTime publishedDate;

    /** 发布人 */
    @Column(name = "published_by", length = 64)
    private String publishedBy;

    /** 版本备注 */
    @Column(name = "remark", length = 512)
    private String remark;

    /** 标签宽度（px） */
    @Column(name = "label_width")
    private Integer labelWidth;

    /** 标签位置：left / top */
    @Column(name = "label_placement", length = 16)
    private String labelPlacement;

    /** 标签对齐方式：left / right */
    @Column(name = "label_align", length = 16)
    private String labelAlign;

    /** 表单尺寸：small / medium / large */
    @Column(name = "form_size", length = 16)
    private String formSize;

    @Override
    public Long getId() {
        return versionId;
    }
}
