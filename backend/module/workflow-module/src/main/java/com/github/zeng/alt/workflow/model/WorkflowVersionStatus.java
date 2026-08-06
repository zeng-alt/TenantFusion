package com.github.zeng.alt.workflow.model;

/**
 * 流程版本状态（草稿 / 已发布 / 已下线）
 *
 * @author zengAlt
 */
public enum WorkflowVersionStatus {

    /** 草稿：仅保存在本地表，未部署到 Camunda */
    DRAFT,

    /** 已发布：已部署到 Camunda 且可用 */
    PUBLISHED,

    /** 已下线：曾被发布，后手动下线停用 */
    OFFLINE
}