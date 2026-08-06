package com.github.zeng.alt.workflow.entity;

import com.github.zeng.alt.domain.base.BaseEntity;
import com.github.zeng.alt.domain.key.SnowflakeId;
import com.github.zeng.alt.workflow.model.FormTemplateVersionStatus;
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
import java.time.LocalDateTime;

/**
 * 表单模板版本表。
 * <p>
 * 每个表单模板（{@link FormTemplateEntity}）对应多条版本记录。
 * 版本状态支持：{@link FormTemplateVersionStatus#DRAFT 草稿} → {@link FormTemplateVersionStatus#PUBLISHED 已发布}
 * → {@link FormTemplateVersionStatus#OFFLINE 已下线}。
 * <p>
 * 草稿版本的 FormDefinition JSON 保存在本地表，发布后成为当前生效版本供表单数据引用。
 *
 * @author zengAlt
 */
@Entity
@Table(name = "wf_form_template_version")
@Getter
@Setter
public class FormTemplateVersionEntity extends BaseEntity<Long> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @SnowflakeId
    @Column(name = "version_id")
    private Long versionId;

    /** 所属表单模板ID */
    @Column(name = "form_template_id", nullable = false)
    private Long formTemplateId;

    /** 版本号（同一模板内从 1 递增） */
    @Column(name = "version", nullable = false)
    private Integer version;

    /** 版本状态 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FormTemplateVersionStatus status = FormTemplateVersionStatus.DRAFT;

    /** 是否当前生效版本（发布后置为 true，新版本发布后原版本置为 false） */
    @Column(name = "is_current")
    private Boolean current = false;

    /** 表单定义（FormKit FormDefinition JSON） */
    @Lob
    @Column(name = "definition", columnDefinition = "CLOB")
    private String definition;

    /** 发布时间 */
    @Column(name = "published_date")
    private LocalDateTime publishedDate;

    /** 发布人 */
    @Column(name = "published_by", length = 64)
    private String publishedBy;

    /** 版本备注 */
    @Column(name = "remark", length = 512)
    private String remark;

    @Override
    public Long getId() {
        return versionId;
    }
}
