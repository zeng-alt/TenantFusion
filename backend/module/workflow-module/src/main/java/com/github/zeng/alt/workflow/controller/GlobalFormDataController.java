package com.github.zeng.alt.workflow.controller;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.workflow.model.GlobalFormDataQuery;
import com.github.zeng.alt.workflow.model.GlobalFormDataSubmitCmd;
import com.github.zeng.alt.workflow.model.GlobalFormDataVO;
import com.github.zeng.alt.workflow.service.GlobalFormDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 流程全局表单数据控制器
 * <p>
 * 提供全局表单提交数据的提交与查询接口（以流程实例ID关联）。
 *
 * @author zengAlt
 */
@CommonsLog
@Tag(name = "流程全局表单数据")
@RestController
@RequestMapping("/v1/global-form-data")
@RequiredArgsConstructor
public class GlobalFormDataController {

    private final GlobalFormDataService globalFormDataService;

    @Operation(summary = "分页查询全局表单数据")
    @GetMapping
    public PageRestResponse<GlobalFormDataVO> page(GlobalFormDataQuery query) {
        return globalFormDataService.page(query);
    }

    @Operation(summary = "提交全局表单数据（同一流程实例重复提交时覆盖）")
    @PostMapping
    public RestResponse<GlobalFormDataVO> submit(@Valid @RequestBody GlobalFormDataSubmitCmd cmd) {
        return RestResponse.success(globalFormDataService.submit(cmd));
    }

    @Operation(summary = "按流程实例ID查询全局表单数据")
    @GetMapping("/{processInstanceId}")
    public RestResponse<GlobalFormDataVO> detail(
            @Parameter(description = "流程实例ID") @PathVariable String processInstanceId) {
        return RestResponse.success(globalFormDataService.getByProcessInstanceId(processInstanceId));
    }
}
