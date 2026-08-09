package com.github.zeng.alt.workflow.controller;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.workflow.model.FormConfigCreateCmd;
import com.github.zeng.alt.workflow.model.FormConfigOptionVO;
import com.github.zeng.alt.workflow.model.FormConfigQuery;
import com.github.zeng.alt.workflow.model.FormConfigSaveDraftCmd;
import com.github.zeng.alt.workflow.model.FormConfigUpdateCmd;
import com.github.zeng.alt.workflow.model.FormConfigVO;
import com.github.zeng.alt.workflow.model.FormConfigVersionVO;
import com.github.zeng.alt.workflow.service.FormConfigService;
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
 * 配置表单管理控制器
 * <p>
 * 提供配置表单主数据 CRUD、草稿保存（含结构化字段）、版本发布、下线等接口。
 *
 * @author zengAlt
 */
@CommonsLog
@Tag(name = "配置表单管理")
@RestController
@RequestMapping("/v1/form-config")
@RequiredArgsConstructor
public class FormConfigController {

    private final FormConfigService formConfigService;

    @Operation(summary = "分页查询配置表单")
    @GetMapping
    public PageRestResponse<FormConfigVO> page(FormConfigQuery query) {
        return formConfigService.page(query);
    }

    @Operation(summary = "查询配置表单下拉选项（供关联选择）")
    @GetMapping("/options")
    public RestResponse<List<FormConfigOptionVO>> options() {
        return RestResponse.success(formConfigService.options());
    }

    @Operation(summary = "获取配置表单详情")
    @GetMapping("/{id}")
    public RestResponse<FormConfigVO> detail(@PathVariable Long id) {
        return RestResponse.success(formConfigService.getDetail(id));
    }

    @Operation(summary = "创建配置表单")
    @PostMapping
    public RestResponse<FormConfigVO> create(@Valid @RequestBody FormConfigCreateCmd cmd) {
        return RestResponse.success(formConfigService.create(cmd));
    }

    @Operation(summary = "更新配置表单主数据")
    @PutMapping("/{id}")
    public RestResponse<FormConfigVO> update(@PathVariable Long id, @Valid @RequestBody FormConfigUpdateCmd cmd) {
        return RestResponse.success(formConfigService.update(id, cmd));
    }

    @Operation(summary = "删除配置表单")
    @DeleteMapping("/{id}")
    public RestResponse<Void> delete(@PathVariable Long id) {
        formConfigService.delete(id);
        return RestResponse.success();
    }

    @Operation(summary = "查询配置表单版本列表")
    @GetMapping("/{id}/versions")
    public RestResponse<List<FormConfigVersionVO>> versions(@PathVariable Long id) {
        return RestResponse.success(formConfigService.versions(id));
    }

    @Operation(summary = "获取配置表单版本详情（含字段）")
    @GetMapping("/versions/{templateId}/{version}")
    public RestResponse<FormConfigVersionVO> versionDetail(@PathVariable Long templateId, @PathVariable Integer version) {
        return RestResponse.success(formConfigService.getVersion(templateId, version));
    }

    @Operation(summary = "获取配置表单版本详情（含字段）")
    @GetMapping("/versions/{versionId}")
    public RestResponse<FormConfigVersionVO> versionDetail(@PathVariable Long versionId) {
        return RestResponse.success(formConfigService.getVersion(versionId));
    }

    @Operation(summary = "保存配置表单草稿（全量替换字段）")
    @PostMapping("/{id}/draft")
    public RestResponse<FormConfigVersionVO> saveDraft(@PathVariable Long id, @Valid @RequestBody FormConfigSaveDraftCmd cmd) {
        return RestResponse.success(formConfigService.saveDraft(id, cmd));
    }

    @Operation(summary = "保存并发布配置表单草稿")
    @PostMapping("/{id}/publish-draft")
    public RestResponse<FormConfigVersionVO> publishDraft(@PathVariable Long id, @Valid @RequestBody FormConfigSaveDraftCmd cmd) {
        return RestResponse.success(formConfigService.saveAndPublish(id, cmd));
    }

    @Operation(summary = "发布配置表单版本")
    @PostMapping("/versions/{versionId}/publish")
    public RestResponse<FormConfigVersionVO> publish(@PathVariable Long versionId) {
        return RestResponse.success(formConfigService.publish(versionId));
    }

    @Operation(summary = "下线配置表单版本")
    @PostMapping("/versions/{versionId}/offline")
    public RestResponse<FormConfigVersionVO> offline(@PathVariable Long versionId) {
        return RestResponse.success(formConfigService.offline(versionId));
    }
}
