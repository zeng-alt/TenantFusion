package com.github.zeng.alt.workflow.controller;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.workflow.model.FormTemplateCreateCmd;
import com.github.zeng.alt.workflow.model.FormTemplateQuery;
import com.github.zeng.alt.workflow.model.FormTemplateSaveDraftCmd;
import com.github.zeng.alt.workflow.model.FormTemplateUpdateCmd;
import com.github.zeng.alt.workflow.model.FormTemplateVO;
import com.github.zeng.alt.workflow.model.FormTemplateVersionVO;
import com.github.zeng.alt.workflow.service.FormTemplateService;
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

import java.util.List;

/**
 * 动态表单模板管理控制器
 * <p>
 * 提供表单模板主数据 CRUD、草稿保存、版本发布、下线等接口，
 * 配合前端 FormKit 表单设计器（formkit-form-builder）使用。
 *
 * @author zengAlt
 */
@CommonsLog
@Tag(name = "动态表单管理")
@RestController
@RequestMapping("/v1/form")
@RequiredArgsConstructor
public class FormTemplateController {

    private final FormTemplateService formTemplateService;

    @Operation(summary = "分页查询表单模板")
    @GetMapping
    public PageRestResponse<FormTemplateVO> page(FormTemplateQuery query) {
        return formTemplateService.page(query);
    }

    @Operation(summary = "获取表单模板详情")
    @GetMapping("/{id}")
    public RestResponse<FormTemplateVO> detail(@PathVariable Long id) {
        return RestResponse.success(formTemplateService.getDetail(id));
    }

    @Operation(summary = "创建表单模板")
    @PostMapping
    public RestResponse<FormTemplateVO> create(@Valid @RequestBody FormTemplateCreateCmd cmd) {
        return RestResponse.success(formTemplateService.create(cmd));
    }

    @Operation(summary = "更新表单模板主数据")
    @PutMapping("/{id}")
    public RestResponse<FormTemplateVO> update(@PathVariable Long id, @Valid @RequestBody FormTemplateUpdateCmd cmd) {
        return RestResponse.success(formTemplateService.update(id, cmd));
    }

    @Operation(summary = "删除表单模板")
    @DeleteMapping("/{id}")
    public RestResponse<Void> delete(@PathVariable Long id) {
        formTemplateService.delete(id);
        return RestResponse.success();
    }

    @Operation(summary = "查询表单模板版本列表")
    @GetMapping("/{id}/versions")
    public RestResponse<List<FormTemplateVersionVO>> versions(@PathVariable Long id) {
        return RestResponse.success(formTemplateService.versions(id));
    }

    @Operation(summary = "获取表单模板版本详情")
    @GetMapping("/versions/{templateId}/{version}")
    public RestResponse<FormTemplateVersionVO> versionDetail(@PathVariable Long templateId, @PathVariable Integer version) {
        return RestResponse.success(formTemplateService.getVersion(templateId, version));
    }

    @Operation(summary = "获取表单模板版本详情")
    @GetMapping("/versions/{versionId}")
    public RestResponse<FormTemplateVersionVO> versionDetail(@PathVariable Long versionId) {
        return RestResponse.success(formTemplateService.getVersion(versionId));
    }

    @Operation(summary = "保存表单模板草稿")
    @PostMapping("/{id}/draft")
    public RestResponse<FormTemplateVersionVO> saveDraft(@PathVariable Long id, @Valid @RequestBody FormTemplateSaveDraftCmd cmd) {
        return RestResponse.success(formTemplateService.saveDraft(id, cmd));
    }

    @Operation(summary = "保存并发布表单模板草稿")
    @PostMapping("/{id}/publish-draft")
    public RestResponse<FormTemplateVersionVO> publishDraft(@PathVariable Long id, @Valid @RequestBody FormTemplateSaveDraftCmd cmd) {
        return RestResponse.success(formTemplateService.saveAndPublish(id, cmd));
    }

    @Operation(summary = "上线表单模板版本（草稿或已下线 → 已发布，成为当前生效版本）")
    @PostMapping("/versions/{versionId}/publish")
    public RestResponse<FormTemplateVersionVO> publish(@PathVariable Long versionId) {
        return RestResponse.success(formTemplateService.publish(versionId));
    }

    @Operation(summary = "下线表单模板版本（已发布 → 已下线）")
    @PostMapping("/versions/{versionId}/offline")
    public RestResponse<FormTemplateVersionVO> offline(@PathVariable Long versionId) {
        return RestResponse.success(formTemplateService.offline(versionId));
    }
}