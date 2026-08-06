package com.github.zeng.alt.workflow.entity;

import com.github.zeng.alt.domain.base.BaseEntity;
import com.github.zeng.alt.domain.key.SnowflakeId;
import com.github.zeng.alt.workflow.model.FormDataStatus;
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
 * 动态表单数据表。
 * <p>
 * 记录用户针对某张表单模板（{@link FormTemplateEntity}）提交的数据。
 * 表单字段值以 JSON 形式保存在 {@code data} 列，可关联流程实例（processInstanceId），
 * 支持草稿（DRAFT）与已提交（SUBMITTED）两种状态。
 *
 * @author zengAlt
 */
@Entity
@Table(name = "wf_form_data")
@Getter
@Setter
public class FormDataEntity extends BaseEntity<Long> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @SnowflakeId
    @Column(name = "form_data_id")
    private Long formDataId;

    /** 所属表单模板ID */
    @Column(name = "form_template_id", nullable = false)
    private Long formTemplateId;

    /** 提交时表单模板版本快照 */
    @Column(name = "form_version", nullable = false)
    private Integer formVersion = 1;

    /** 关联流程实例ID（发起流程后回写） */
    @Column(name = "process_instance_id", length = 64)
    private String processInstanceId;

    /** 表单字段值（JSON：字段名 → 值） */
    @Lob
    @Column(name = "data", columnDefinition = "CLOB")
    private String data;

    /** 数据状态 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FormDataStatus status = FormDataStatus.DRAFT;

    /** 提交时间 */
    @Column(name = "submitted_date")
    private LocalDateTime submittedDate;

    /** 备注 */
    @Column(name = "remark", length = 512)
    private String remark;

    @Override
    public Long getId() {
        return formDataId;
    }
}
