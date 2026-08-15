package com.github.zeng.alt.workflow.controller;

import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.camunda.engine.api.process.ProcessInformation;
import com.github.zeng.alt.workflow.model.StartByDefinitionAtElementCmd;
import com.github.zeng.alt.workflow.model.StartByMessageAtElementCmd;
import com.github.zeng.alt.workflow.model.StartByMessageCmd;
import com.github.zeng.alt.workflow.model.StartByProcessDefinitionCmd;
import com.github.zeng.alt.workflow.service.WorkflowStarterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 流程启动控制器
 *
 * @author zengAlt
 */
@CommonsLog
@Tag(name = "流程启动")
@RestController
@RequestMapping("/v1/workflow/starter")
@RequiredArgsConstructor
public class WorkflowStarterController {

    private final WorkflowStarterService workflowStarterService;

    @Operation(summary = "按流程定义Key启动流程")
    @PostMapping("/definition")
    public RestResponse<ProcessInformation> startByProcessDefinition(@Valid @RequestBody StartByProcessDefinitionCmd cmd) {
        return RestResponse.success(workflowStarterService.startByProcessDefinition(cmd));
    }

    @Operation(summary = "按消息名称启动流程")
    @PostMapping("/message")
    public RestResponse<ProcessInformation> startByMessage(@Valid @RequestBody StartByMessageCmd cmd) {
        return RestResponse.success(workflowStarterService.startByMessage(cmd));
    }

    @Operation(summary = "按流程定义Key在指定节点启动流程")
    @PostMapping("/definition/at-element")
    public RestResponse<ProcessInformation> startByDefinitionAtActivity(@Valid @RequestBody StartByDefinitionAtElementCmd cmd) {
        return RestResponse.success(workflowStarterService.startByDefinitionAtActivity(cmd));
    }

    @Operation(summary = "按消息名称在指定节点启动流程")
    @PostMapping("/message/at-element")
    public RestResponse<ProcessInformation> startByMessageAtActivity(@Valid @RequestBody StartByMessageAtElementCmd cmd) {
        return RestResponse.success(workflowStarterService.startByMessageAtActivity(cmd));
    }
}
