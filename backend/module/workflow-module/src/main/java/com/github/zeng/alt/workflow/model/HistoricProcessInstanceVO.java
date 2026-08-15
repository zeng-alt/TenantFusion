package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 历史流程实例视图对象
 *
 * @author zengAlt
 */
@Data
@Builder
@Schema(name = "历史流程实例")
public class HistoricProcessInstanceVO implements Serializable {

    @Schema(name = "流程实例ID")
    private String id;

    @Schema(name = "业务键")
    private String businessKey;

    @Schema(name = "流程定义Key")
    private String processDefinitionKey;

    @Schema(name = "流程定义名称")
    private String processDefinitionName;

    @Schema(name = "流程定义版本")
    private Integer processDefinitionVersion;

    @Schema(name = "开始时间")
    private LocalDateTime startTime;

    @Schema(name = "结束时间")
    private LocalDateTime endTime;

    @Schema(name = "持续时长（毫秒）")
    private Long durationInMillis;

    @Schema(name = "启动用户ID")
    private String startUserId;

    @Schema(name = "启动用户名称")
    private String startUserName;

    @Schema(name = "结束状态：active-进行中，completed-正常完成，deleted-已删除，suspended-已挂起")
    private String state;

    @Schema(name = "流程状态：running-进行中，completed-已完成，terminated-已终止，suspended-已挂起")
    private String status;

    @Schema(name = "当前节点名称")
    private String currentTaskName;

    @Schema(name = "当前处理人")
    private String currentAssignee;

    @Schema(name = "删除原因")
    private String deleteReason;

    @Schema(name = "租户ID")
    private String tenantId;

    @Schema(name = "流程定义原始 BPMN XML")
    private String bpmnXml;

    @Schema(name = "发起时提交的表单数据（processForm）")
    private Object processForm;

    @Schema(name = "当前任务节点表单定义列表")
    private List<TaskFormDefinitionVO> currentTaskForms;

    @Schema(name = "配置表单定义（流程绑定业务关联的配置表单版本）")
    private FormConfigVersionVO configForm;

    @Schema(name = "流程执行状态（供 BpmnProcessViewer 高亮/时间线使用）")
    private ProcessExecutionState executionState;
}
