package com.github.zeng.alt.workflow.controller;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.workflow.model.HistoricActivityVO;
import com.github.zeng.alt.workflow.model.HistoricProcessInstanceQuery;
import com.github.zeng.alt.workflow.model.HistoricProcessInstanceVO;
import com.github.zeng.alt.workflow.model.HistoricVariableVO;
import com.github.zeng.alt.workflow.model.TaskVO;
import com.github.zeng.alt.workflow.service.WorkflowHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作流历史控制器
 *
 * @author zengAlt
 */
@CommonsLog
@Tag(name = "工作流历史")
@RestController
@RequestMapping("/v1/workflow/history")
@RequiredArgsConstructor
public class WorkflowHistoryController {

    private final WorkflowHistoryService workflowHistoryService;

    @Operation(summary = "分页查询历史流程实例")
    @GetMapping("/process-instances")
    public PageRestResponse<HistoricProcessInstanceVO> listHistoricInstances(HistoricProcessInstanceQuery query) {
        return workflowHistoryService.queryHistoricInstances(query);
    }

    @Operation(summary = "获取历史流程实例详情")
    @GetMapping("/process-instances/{id}")
    public RestResponse<HistoricProcessInstanceVO> historicInstanceDetail(@PathVariable String id) {
        return RestResponse.success(workflowHistoryService.getHistoricInstance(id));
    }

    @Operation(summary = "分页查询历史任务")
    @GetMapping("/tasks")
    public PageRestResponse<TaskVO> listHistoricTasks(
            @Parameter(description = "办理人") @RequestParam(required = false) String assignee,
            @Parameter(description = "流程实例ID") @RequestParam(required = false) String processInstanceId,
            @Parameter(description = "是否已完成") @RequestParam(required = false) Boolean finished,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize) {
        return workflowHistoryService.queryHistoricTasks(assignee, processInstanceId, finished, pageNum, pageSize);
    }

    @Operation(summary = "查询流程实例的历史活动节点（审批链路）")
    @GetMapping("/activities")
    public RestResponse<List<HistoricActivityVO>> listHistoricActivities(
            @Parameter(description = "流程实例ID", required = true) @RequestParam String processInstanceId) {
        return RestResponse.success(workflowHistoryService.queryHistoricActivities(processInstanceId));
    }

    @Operation(summary = "查询历史变量变更")
    @GetMapping("/variables")
    public PageRestResponse<HistoricVariableVO> listHistoricVariables(
            @Parameter(description = "流程实例ID", required = true) @RequestParam String processInstanceId,
            @Parameter(description = "变量名") @RequestParam(required = false) String variableName,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize) {
        return workflowHistoryService.queryHistoricVariables(processInstanceId, variableName, pageNum, pageSize);
    }
}
