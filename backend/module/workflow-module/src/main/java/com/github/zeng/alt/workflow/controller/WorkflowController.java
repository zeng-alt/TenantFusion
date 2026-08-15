package com.github.zeng.alt.workflow.controller;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.workflow.model.*;
import com.github.zeng.alt.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * 流程管理控制器
 * <p>
 * 提供流程主数据 CRUD、草稿保存、版本发布、下线、挂起/激活等接口。
 *
 * @author zengAlt
 */
@CommonsLog
@Tag(name = "流程管理")
@RestController
@RequestMapping("/v1/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @Operation(summary = "分页查询流程")
    @GetMapping
    public PageRestResponse<WorkflowVO> page(WorkflowQuery query) {
        return workflowService.page(query);
    }

    @Operation(summary = "获取流程详情")
    @GetMapping("/{id}")
    public RestResponse<WorkflowVO> detail(@PathVariable Long id) {
        return RestResponse.success(workflowService.getDetail(id));
    }

    @Operation(summary = "创建流程")
    @PostMapping
    public RestResponse<WorkflowVO> create(@Valid @RequestBody WorkflowCreateCmd cmd) {
        return RestResponse.success(workflowService.create(cmd));
    }

    @Operation(summary = "更新流程主数据")
    @PutMapping("/{id}")
    public RestResponse<WorkflowVO> update(@PathVariable Long id, @Valid @RequestBody WorkflowUpdateCmd cmd) {
        return RestResponse.success(workflowService.update(id, cmd));
    }

    @Operation(summary = "删除流程")
    @DeleteMapping("/{id}")
    public RestResponse<Void> delete(@PathVariable Long id) {
        workflowService.delete(id);
        return RestResponse.success();
    }

    @Operation(summary = "查询流程版本列表")
    @GetMapping("/{id}/versions")
    public RestResponse<List<WorkflowVersionVO>> versions(@PathVariable Long id) {
        return RestResponse.success(workflowService.versions(id));
    }

    @Operation(summary = "获取版本详情")
    @GetMapping("/versions/{versionId}")
    public RestResponse<WorkflowVersionVO> versionDetail(@PathVariable Long versionId) throws IOException {
        return RestResponse.success(workflowService.getVersion(versionId));
    }

    @Operation(summary = "获取流程模板版本详情")
    @GetMapping("/versions/{templateId}/{version}")
    public RestResponse<WorkflowVersionVO> versionDetail(@PathVariable Long templateId, @PathVariable Integer version) {
        return RestResponse.success(workflowService.getVersion(templateId, version));
    }

    @Operation(summary = "保存流程草稿")
    @PostMapping("/{id}/draft")
    public RestResponse<WorkflowVersionVO> saveDraft(@PathVariable Long id, @Valid @RequestBody WorkflowSaveDraftCmd cmd) {
        return RestResponse.success(workflowService.saveDraft(id, cmd));
    }

    @Operation(summary = "保存并发布流程")
    @PostMapping("/{id}/publish")
    public RestResponse<WorkflowVersionVO> saveDraftAndPublish(@PathVariable Long id, @Valid @RequestBody WorkflowSaveDraftCmd cmd) {
        return RestResponse.success(workflowService.saveDraftAndPublish(id, cmd));
    }

    @Operation(summary = "上线流程版本（草稿或已下线 → 已发布，成为当前生效版本）")
    @PostMapping("/versions/{versionId}/publish")
    public RestResponse<WorkflowVersionVO> publish(@PathVariable Long versionId) {
        return RestResponse.success(workflowService.publish(versionId));
    }

    @Operation(summary = "挂起流程")
    @PostMapping("/{id}/offline")
    public RestResponse<Void> offline(@PathVariable Long id) {
        workflowService.offline(id);
        return RestResponse.success();
    }
}
