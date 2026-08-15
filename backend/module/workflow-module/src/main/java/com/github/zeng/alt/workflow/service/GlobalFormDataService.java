package com.github.zeng.alt.workflow.service;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.workflow.model.GlobalFormDataQuery;
import com.github.zeng.alt.workflow.model.GlobalFormDataSubmitCmd;
import com.github.zeng.alt.workflow.model.GlobalFormDataVO;
import com.github.zeng.alt.workflow.model.GlobalFormDefinitionVO;

/**
 * 流程全局表单数据服务接口
 *
 * @author zengAlt
 */
public interface GlobalFormDataService {

    /**
     * 分页查询全局表单数据
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageRestResponse<GlobalFormDataVO> page(GlobalFormDataQuery query);

    /**
     * 发起流程时初始化全局表单数据（空数据 + 表单定义快照）。
     * CAMUNDA 类型不保存 FormKit 定义，每次实时解析最新版本；EXTERNAL/GENERATED 保存其定义。
     *
     * @param processInstanceId 流程实例ID
     * @param workflowCode      流程模板编码
     * @param definition        全局表单定义快照
     * @return 初始化后的数据
     */
    GlobalFormDataVO initialize(String processInstanceId, String workflowCode, GlobalFormDefinitionVO definition);

    /**
     * 提交全局表单数据（同一流程实例多次提交时更新原记录）
     *
     * @param cmd 提交命令
     * @return 保存后的数据
     */
    GlobalFormDataVO submit(GlobalFormDataSubmitCmd cmd);

    /**
     * 按流程实例ID查询最近一次提交的全局表单数据
     *
     * @param processInstanceId 流程实例ID
     * @return 全局表单数据（未提交过时返回 null）
     */
    GlobalFormDataVO getByProcessInstanceId(String processInstanceId);
}
