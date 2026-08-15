package com.github.zeng.alt.workflow.entity;

import com.github.zeng.alt.domain.base.BaseEntity;
import com.github.zeng.alt.domain.key.SnowflakeId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 流程全局表单数据表。
 * <p>
 * 记录流程运行过程中全局表单（由流程级 camunda:property globalForm.* 定义）提交的数据，
 * 以流程实例ID（{@code processInstanceId}）与流程模板编码（{@code workflowCode}）关联，
 * 字段值以 JSON 形式保存在 {@code data} 列。
 * <p>
 * 同一流程实例多次提交时更新原记录（每次提交覆盖 data 并刷新提交时间）。
 *
 * @author zengAlt
 */
@Entity
@Table(name = "wf_global_form_data")
@Getter
@Setter
public class GlobalFormDataEntity extends BaseEntity<Long> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @SnowflakeId
    @Column(name = "global_form_data_id")
    private Long globalFormDataId;

    /** 运行时流程实例ID */
    @Column(name = "process_instance_id", nullable = false, length = 64)
    private String processInstanceId;

    /** 流程模板编码 */
    @Column(name = "workflow_code", length = 128)
    private String workflowCode;

    /** 全局表单字段值（JSON：字段名 → 值） */
    @Lob
    @Column(name = "data", columnDefinition = "CLOB")
    private String data;

    /** 发起流程时保存的全局表单定义快照（JSON；CAMUNDA 类型不保存 FormKit 定义，每次实时解析最新版本） */
    @Lob
    @Column(name = "definition", columnDefinition = "CLOB")
    private String definition;

    /** 提交时间 */
    @Column(name = "submitted_date")
    private LocalDateTime submittedDate;

    @Override
    public Long getId() {
        return globalFormDataId;
    }
}
