package com.github.zeng.alt.workflow.controller;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.workflow.model.FormDataCreateCmd;
import com.github.zeng.alt.workflow.model.FormDataQuery;
import com.github.zeng.alt.workflow.model.FormDataUpdateCmd;
import com.github.zeng.alt.workflow.model.FormDataValidateCmd;
import com.github.zeng.alt.workflow.model.FormDataVO;
import com.github.zeng.alt.workflow.service.FormDataService;
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

import java.util.Map;

/**
 * 动态表单数据控制器
 * <p>
 * 提供表单提交数据（字段值）的 CRUD 接口。
 *
 * @author zengAlt
 */
@CommonsLog
@Tag(name = "动态表单数据管理")
@RestController
@RequestMapping("/v1/form-data")
@RequiredArgsConstructor
public class FormDataController {

    private final FormDataService formDataService;

    @Operation(summary = "分页查询表单数据")
    @GetMapping
    public PageRestResponse<FormDataVO> page(FormDataQuery query) {
        return formDataService.page(query);
    }

    @Operation(summary = "获取表单数据详情")
    @GetMapping("/{id}")
    public RestResponse<FormDataVO> detail(@PathVariable Long id) {
        return RestResponse.success(formDataService.getDetail(id));
    }

    @Operation(summary = "创建表单数据")
    @PostMapping
    public RestResponse<FormDataVO> create(@Valid @RequestBody FormDataCreateCmd cmd) {
        return RestResponse.success(formDataService.create(cmd));
    }

    @Operation(summary = "校验表单数据（按已发布定义的校验规则 + 条件显示，不落库）")
    @PostMapping("/validate")
    public RestResponse<Map<String, String>> validate(@Valid @RequestBody FormDataValidateCmd cmd) {
        return RestResponse.success(formDataService.validate(cmd));
    }

    @Operation(summary = "更新表单数据")
    @PutMapping("/{id}")
    public RestResponse<FormDataVO> update(@PathVariable Long id, @Valid @RequestBody FormDataUpdateCmd cmd) {
        return RestResponse.success(formDataService.update(id, cmd));
    }

    @Operation(summary = "删除表单数据")
    @DeleteMapping("/{id}")
    public RestResponse<Void> delete(@PathVariable Long id) {
        formDataService.delete(id);
        return RestResponse.success();
    }
}