package com.github.zeng.alt.workflow.model;

/**
 * 节点/连线执行状态（供 BpmnProcessViewer 高亮渲染）
 *
 * @author zengAlt
 */
public enum ExecutionStatus {
    pending,
    active,
    completed,
    rejected
}
