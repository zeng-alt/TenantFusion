package com.github.zeng.alt.workflow.controller;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.workflow.model.ProcessInstanceQuery;
import com.github.zeng.alt.workflow.model.ProcessInstanceVO;
import com.github.zeng.alt.workflow.model.StartProcessCmd;
import com.github.zeng.alt.workflow.service.ProcessInstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 流程实例控制器
 *
 * @author zengAlt
 */
@CommonsLog
@Tag(name = "流程实例")
@RestController
@RequestMapping("/v1/workflow/instances")
@RequiredArgsConstructor
public class ProcessInstanceController {

    private final ProcessInstanceService processInstanceService;

    @Operation(summary = "分页查询流程实例")
    @GetMapping
    public PageRestResponse<ProcessInstanceVO> list(@Valid ProcessInstanceQuery query) {
        return processInstanceService.queryInstances(query);
    }

    @Operation(summary = "获取流程实例详情")
    @GetMapping("/{id}")
    public RestResponse<ProcessInstanceVO> detail(@PathVariable String id) {
        return RestResponse.success(processInstanceService.getInstance(id));
    }

    @Operation(summary = "启动流程")
    @PostMapping
    public RestResponse<ProcessInstanceVO> start(@Valid @RequestBody StartProcessCmd cmd) {
        return RestResponse.success(processInstanceService.startProcess(cmd));
    }

    @Operation(summary = "挂起流程实例")
    @PostMapping("/{id}/suspend")
    public RestResponse<Void> suspend(@PathVariable String id) {
        processInstanceService.suspendInstance(id);
        return RestResponse.success();
    }

    @Operation(summary = "激活流程实例")
    @PostMapping("/{id}/activate")
    public RestResponse<Void> activate(@PathVariable String id) {
        processInstanceService.activateInstance(id);
        return RestResponse.success();
    }

    @Operation(summary = "删除/终止流程实例")
    @DeleteMapping("/{id}")
    public RestResponse<Void> delete(
            @PathVariable String id,
            @Parameter(description = "删除原因") @RequestParam(defaultValue = "手动终止") String reason) {
        processInstanceService.deleteInstance(id, reason);
        return RestResponse.success();
    }

    @Operation(summary = "获取流程变量")
    @GetMapping("/{id}/variables")
    public RestResponse<Map<String, Object>> variables(@PathVariable String id) {
        return RestResponse.success(processInstanceService.getVariables(id));
    }

    @Operation(summary = "设置流程变量")
    @PutMapping("/{id}/variables")
    public RestResponse<Void> setVariables(
            @PathVariable String id,
            @RequestBody Map<String, Object> variables) {
        processInstanceService.setVariables(id, variables);
        return RestResponse.success();
    }
}
