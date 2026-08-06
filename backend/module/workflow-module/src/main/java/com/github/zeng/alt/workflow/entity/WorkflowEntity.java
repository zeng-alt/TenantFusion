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
 * 流程主表。
 * <p>
 * 记录一条可管理、可设计、可发布、可迭代的流程定义主数据。
 * 每个流程拥有多条版本记录（{@link WorkflowVersionEntity}），
 * 支持草稿、发布、下线等状态流转与版本管理。
 *
 * @author zengAlt
 */
@Entity
@Table(name = "wf_workflow")
@Getter
@Setter
public class WorkflowEntity extends BaseEntity<Long> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @SnowflakeId
    @Column(name = "workflow_id")
    private Long workflowId;

    /** 流程编码（全局唯一），对应 Camunda processDefinitionKey */
    @Column(name = "workflow_key", nullable = false, unique = true, length = 128)
    private String workflowKey;

    /** 流程名称 */
    @Column(name = "workflow_name", nullable = false, length = 128)
    private String workflowName;

    /** 流程描述 */
    @Column(name = "description", length = 512)
    private String description;

    /** 流程分类（如：人事 / 财务 / 行政） */
    @Column(name = "category", length = 64)
    private String category;

    /** 当前生效的已发布版本号（0 表示从未发布） */
    @Column(name = "current_version")
    private Integer currentVersion = 0;

    /** 最新版本号（含草稿） */
    @Column(name = "latest_version")
    private Integer latestVersion = 0;

    /** 备注 */
    @Column(name = "remark", length = 512)
    private String remark;

    @Override
    public Long getId() {
        return workflowId;
    }
}
