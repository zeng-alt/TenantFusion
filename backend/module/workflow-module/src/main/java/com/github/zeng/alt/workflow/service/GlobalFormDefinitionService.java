package com.github.zeng.alt.workflow.service;

import com.github.zeng.alt.workflow.model.GlobalFormDefinitionVO;

/**
 * 流程全局表单定义服务接口
 *
 * @author zengAlt
 */
public interface GlobalFormDefinitionService {

    /**
     * 按流程模板编码解析当前生效版本定义的全局表单
     *
     * @param workflowCode 流程模板编码
     * @return 全局表单定义（流程不存在、未配置全局表单时返回 null）
     */
    GlobalFormDefinitionVO resolveByWorkflowCode(String workflowCode);
}
