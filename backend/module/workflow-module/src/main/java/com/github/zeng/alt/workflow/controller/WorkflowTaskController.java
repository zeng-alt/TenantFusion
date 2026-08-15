package com.github.zeng.alt.workflow.controller;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.workflow.model.*;
import com.github.zeng.alt.workflow.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户任务控制器
 *
 * @author zengAlt
 */
@CommonsLog
@Tag(name = "用户任务")
@RestController
@RequestMapping("/v1/workflow/tasks")
@RequiredArgsConstructor
public class WorkflowTaskController {

    private final TaskService taskService;

    @Operation(summary = "分页查询任务")
    @GetMapping
    public PageRestResponse<TaskVO> list(@Valid TaskQuery query) {
        return taskService.queryTasks(query);
    }

    @Operation(summary = "获取任务详情")
    @GetMapping("/{id}")
    public RestResponse<TaskVO> detail(@PathVariable String id) {
        return RestResponse.success(taskService.getTask(id));
    }

    @Operation(summary = "获取任务表单定义")
    @GetMapping("/{id}/forms")
    public RestResponse<List<TaskFormDefinitionVO>> forms(@PathVariable String id) {
        return RestResponse.success(taskService.getTaskForms(id));
    }

    @Operation(summary = "签收/认领任务")
    @PostMapping("/{id}/claim")
    public RestResponse<Void> claim(
            @PathVariable String id,
            @RequestBody(required = false) ClaimTaskCmd cmd) {
        String userId = (cmd != null && cmd.getUserId() != null) ? cmd.getUserId() : "";
        taskService.claimTask(id, userId);
        return RestResponse.success();
    }

    @Operation(summary = "取消认领")
    @PostMapping("/{id}/unclaim")
    public RestResponse<Void> unclaim(@PathVariable String id) {
        taskService.unclaimTask(id);
        return RestResponse.success();
    }

    @Operation(summary = "完成任务")
    @PostMapping("/complete")
    public RestResponse<Void> complete(@Valid @RequestBody CompleteTaskCmd cmd) {
        taskService.completeTask(cmd);
        return RestResponse.success();
    }

    @Operation(summary = "委派任务")
    @PostMapping("/{id}/delegate")
    public RestResponse<Void> delegate(
            @PathVariable String id,
            @Valid @RequestBody DelegateTaskCmd cmd) {
        taskService.delegateTask(id, cmd.getUserId());
        return RestResponse.success();
    }

    @Operation(summary = "解决委派任务（被委派人处理完成回退）")
    @PostMapping("/{id}/resolve")
    public RestResponse<Void> resolve(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> variables) {
        taskService.resolveTask(id, variables);
        return RestResponse.success();
    }

    @Operation(summary = "分配任务办理人")
    @PostMapping("/{id}/assign")
    public RestResponse<Void> assign(
            @PathVariable String id,
            @Parameter(description = "目标用户ID") @RequestParam String userId) {
        taskService.assignTask(id, userId);
        return RestResponse.success();
    }

    @Operation(summary = "获取任务批注列表")
    @GetMapping("/{id}/comments")
    public PageRestResponse<TaskCommentVO> comments(
            @PathVariable String id,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize) {
        return taskService.getComments(id, pageNum, pageSize);
    }

    @Operation(summary = "添加任务批注")
    @PostMapping("/{id}/comments")
    public RestResponse<Void> addComment(
            @PathVariable String id,
            @Parameter(description = "流程实例ID") @RequestParam(required = false) String processInstanceId,
            @RequestBody String message) {
        taskService.addComment(id, processInstanceId, message);
        return RestResponse.success();
    }
}
