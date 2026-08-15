package com.github.zeng.alt.workflow.controller;

import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.workflow.model.GlobalFormDefinitionVO;
import com.github.zeng.alt.workflow.service.GlobalFormDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 流程全局表单定义控制器
 * <p>
 * 按流程模板编码解析当前生效版本定义的全局表单（供数据预览使用）。
 *
 * @author zengAlt
 */
@CommonsLog
@Tag(name = "流程全局表单定义")
@RestController
@RequestMapping("/v1/global-form")
@RequiredArgsConstructor
public class GlobalFormDefinitionController {

    private final GlobalFormDefinitionService globalFormDefinitionService;

    @Operation(summary = "按流程模板编码解析全局表单定义")
    @GetMapping("/definition")
    public RestResponse<GlobalFormDefinitionVO> definition(
            @Parameter(description = "流程模板编码") @RequestParam String workflowCode) {
        return RestResponse.success(globalFormDefinitionService.resolveByWorkflowCode(workflowCode));
    }
}
