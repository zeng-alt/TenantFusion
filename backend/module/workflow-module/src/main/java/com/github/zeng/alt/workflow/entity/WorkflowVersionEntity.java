package com.github.zeng.alt.workflow.entity;

import com.github.zeng.alt.domain.base.BaseEntity;
import com.github.zeng.alt.domain.key.SnowflakeId;
import com.github.zeng.alt.workflow.model.WorkflowVersionStatus;
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
 * 流程版本表。
 * <p>
 * 每个流程（{@link WorkflowEntity}）对应多条版本记录。
 * 版本状态支持：{@link WorkflowVersionStatus#DRAFT 草稿} → {@link WorkflowVersionStatus#PUBLISHED 已发布}
 * → {@link WorkflowVersionStatus#OFFLINE 已下线}。
 * <p>
 * 草稿版本（{@link WorkflowVersionStatus#DRAFT}）的 BPMN XML 保存在本地表，
 * 发布时自动部署到 Camunda 并回写部署 ID，同时清空本地 XML（已发布版本不再保存 XML，
 * 设计器读取时从 Camunda 部署模型中加载）。
 *
 * @author zengAlt
 */
@Entity
@Table(name = "wf_workflow_version")
@Getter
@Setter
public class WorkflowVersionEntity extends BaseEntity<Long> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @SnowflakeId
    @Column(name = "version_id")
    private Long versionId;

    /** 所属流程ID */
    @Column(name = "workflow_id", nullable = false)
    private Long workflowId;

    /** 版本号（同一流程内从 1 递增） */
    @Column(name = "version", nullable = false)
    private Integer version;

    /** 版本状态 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WorkflowVersionStatus status = WorkflowVersionStatus.DRAFT;

    /** （是否当前生效版本发布后置为 true，新版本发布后原版本置为 false） */
    @Column(name = "is_current")
    private Boolean current = false;

    /** BPMN XML 内容 */
    @Lob
    @Column(name = "bpmn_xml", columnDefinition = "CLOB")
    private String bpmnXml;

    /** Camunda 部署 ID（发布时回写） */
    @Column(name = "deployment_id", length = 64)
    private String deploymentId;

    /** Camunda 流程定义 ID（发布时回写） */
    @Column(name = "process_definition_id", length = 64)
    private String processDefinitionId;

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
