package com.github.zeng.alt.workflow.controller;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.camunda.engine.api.deploy.DeploymentInformation;
import com.github.zeng.alt.workflow.model.ProcessDefinitionVO;
import com.github.zeng.alt.workflow.service.ProcessDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 流程定义控制器
 *
 * @author zengAlt
 */
@CommonsLog
@Tag(name = "流程定义")
@RestController
@RequestMapping("/v1/workflow/definitions")
@RequiredArgsConstructor
public class ProcessDefinitionController {

    private final ProcessDefinitionService processDefinitionService;

//    @Operation(summary = "分页查询流程定义")
//    @GetMapping
//    public PageRestResponse<ProcessDefinitionVO> list(
//            @Parameter(description = "流程定义Key") @RequestParam(required = false) String key,
//            @Parameter(description = "流程定义名称") @RequestParam(required = false) String name,
//            @Parameter(description = "是否挂起") @RequestParam(required = false) Boolean suspended,
//            @Parameter(description = "是否仅最新版本") @RequestParam(required = false, defaultValue = "true") Boolean latestVersion,
//            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
//            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize) {
//        return processDefinitionService.queryDefinitions(key, name, suspended, latestVersion, pageNum, pageSize);
//    }

    @Operation(summary = "获取流程定义详情")
    @GetMapping("/{id}")
    public RestResponse<ProcessDefinitionVO> detail(@PathVariable String id) {
        return RestResponse.success(processDefinitionService.getDefinition(id));
    }

    @Operation(summary = "部署流程定义")
    @PostMapping("/deploy")
    public RestResponse<DeploymentInformation> deploy(@RequestParam("file") MultipartFile bpmnXml) {
        try {
            return RestResponse.success(processDefinitionService.deploy(bpmnXml));
        } catch (IOException exception) {
            log.error(exception);
            return RestResponse.fail();
        }
    }

    @Operation(summary = "删除流程定义")
    @DeleteMapping("/{id}")
    public RestResponse<Void> delete(
            @PathVariable String id,
            @Parameter(description = "是否级联删除") @RequestParam(defaultValue = "false") boolean cascade) {
        processDefinitionService.deleteDefinition(id, cascade);
        return RestResponse.success();
    }

    @Operation(summary = "挂起流程定义")
    @PostMapping("/{id}/suspend")
    public RestResponse<Void> suspend(@PathVariable String id) {
        processDefinitionService.suspendDefinition(id);
        return RestResponse.success();
    }

    @Operation(summary = "激活流程定义")
    @PostMapping("/{id}/activate")
    public RestResponse<Void> activate(@PathVariable String id) {
        processDefinitionService.activateDefinition(id);
        return RestResponse.success();
    }

    @Operation(summary = "获取流程定义版本历史")
    @GetMapping("/{key}/versions")
    public RestResponse<List<ProcessDefinitionVO>> versions(
            @Parameter(description = "流程定义Key") @PathVariable String key) {
        return RestResponse.success(processDefinitionService.getVersions(key));
    }

    @Operation(summary = "获取流程定义BPMN XML")
    @GetMapping("/{id}/bpmn-xml")
    public RestResponse<String> bpmnXml(@PathVariable String id) {
        return RestResponse.success(processDefinitionService.getBpmnXml(id));
    }
}
