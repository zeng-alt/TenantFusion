package com.github.zeng.alt.workflow.controller;

import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.workflow.model.BusinessCreateCmd;
import com.github.zeng.alt.workflow.model.BusinessUpdateCmd;
import com.github.zeng.alt.workflow.model.BusinessVO;
import com.github.zeng.alt.workflow.model.FormConfigCreateCmd;
import com.github.zeng.alt.workflow.service.BusinessService;
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
 * 业务管理控制器
 * <p>
 * 提供业务树节点的扁平列表、树形结构、详情、创建、更新、删除等接口。
 *
 * @author zengAlt
 */
@CommonsLog
@Tag(name = "业务管理")
@RestController
@RequestMapping("/v1/business")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessService businessService;

    @Operation(summary = "查询业务扁平列表")
    @GetMapping
    public RestResponse<List<BusinessVO>> list() {
        return RestResponse.success(businessService.list());
    }

    @Operation(summary = "查询业务树")
    @GetMapping("/tree")
    public RestResponse<List<BusinessVO>> tree() {
        return RestResponse.success(businessService.tree());
    }

    @Operation(summary = "获取业务详情")
    @GetMapping("/{id}")
    public RestResponse<BusinessVO> detail(@PathVariable Long id) {
        return RestResponse.success(businessService.getDetail(id));
    }

    @Operation(summary = "创建业务")
    @PostMapping
    public RestResponse<BusinessVO> create(@Valid @RequestBody BusinessCreateCmd cmd) {
        return RestResponse.success(businessService.create(cmd));
    }

    @Operation(summary = "更新业务")
    @PutMapping("/{id}")
    public RestResponse<BusinessVO> update(@PathVariable Long id, @Valid @RequestBody BusinessUpdateCmd cmd) {
        return RestResponse.success(businessService.update(id, cmd));
    }

    @Operation(summary = "删除业务")
    @DeleteMapping("/{id}")
    public RestResponse<Void> delete(@PathVariable Long id) {
        businessService.delete(id);
        return RestResponse.success();
    }

    @Operation(summary = "创建配置表单并关联到业务")
    @PostMapping("/{id}/form-config")
    public RestResponse<BusinessVO> createAndBindFormConfig(
            @PathVariable Long id, @Valid @RequestBody FormConfigCreateCmd cmd) {
        return RestResponse.success(businessService.createAndBindFormConfig(id, cmd));
    }
}
