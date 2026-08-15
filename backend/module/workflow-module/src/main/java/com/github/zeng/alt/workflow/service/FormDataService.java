package com.github.zeng.alt.workflow.service;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.workflow.model.FormDataCreateCmd;
import com.github.zeng.alt.workflow.model.FormDataQuery;
import com.github.zeng.alt.workflow.model.FormDataUpdateCmd;
import com.github.zeng.alt.workflow.model.FormDataValidateCmd;
import com.github.zeng.alt.workflow.model.FormDataVO;

import java.util.Map;

/**
 * 动态表单数据服务接口
 * <p>
 * 提供表单数据的分页查询、详情、创建、更新（含部分更新）与删除能力。
 *
 * @author zengAlt
 */
public interface FormDataService {

    /**
     * 分页查询表单数据
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageRestResponse<FormDataVO> page(FormDataQuery query);

    /**
     * 获取表单数据详情（含字段值）
     *
     * @param id 数据ID
     * @return 表单数据
     */
    FormDataVO getDetail(Long id);

    /**
     * 创建表单数据
     *
     * @param cmd 创建命令
     * @return 创建后的表单数据
     */
    FormDataVO create(FormDataCreateCmd cmd);

    /**
     * 更新表单数据（部分更新：null 字段不修改）
     *
     * @param id  数据ID
     * @param cmd 更新命令
     * @return 更新后的表单数据
     */
    FormDataVO update(Long id, FormDataUpdateCmd cmd);

    /**
     * 删除表单数据
     *
     * @param id 数据ID
     */
    void delete(Long id);

    /**
     * 校验表单数据（按已发布定义的校验规则 + 条件显示，不落库）
     *
     * @param cmd 校验命令
     * @return 字段名 → 错误文案；合法时为空表
     */
    Map<String, String> validate(FormDataValidateCmd cmd);
}