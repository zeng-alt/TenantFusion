package com.github.zeng.alt.workflow.service;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.workflow.model.FormTemplateCreateCmd;
import com.github.zeng.alt.workflow.model.FormTemplatePublishedVO;
import com.github.zeng.alt.workflow.model.FormTemplateQuery;
import com.github.zeng.alt.workflow.model.FormTemplateSaveDraftCmd;
import com.github.zeng.alt.workflow.model.FormTemplateUpdateCmd;
import com.github.zeng.alt.workflow.model.FormTemplateVO;
import com.github.zeng.alt.workflow.model.FormTemplateVersionVO;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * 动态表单模板服务接口
 * <p>
 * 提供表单模板的分页查询、详情、创建、更新（含部分更新）、删除，
 * 以及版本管理（列表、详情、保存草稿、发布、下线）能力。
 *
 * @author zengAlt
 */
public interface FormTemplateService {

    /**
     * 分页查询表单模板
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageRestResponse<FormTemplateVO> page(FormTemplateQuery query);

    /**
     * 获取表单模板详情
     *
     * @param id 模板ID
     * @return 表单模板
     */
    FormTemplateVO getDetail(Long id);

    /**
     * 按模板编码获取运行态表单定义（当前已发布版本，未发布时抛业务异常）
     *
     * @param code 模板编码
     * @return 运行态表单定义
     */
    FormTemplatePublishedVO getPublishedByCode(String code);

    /**
     * 按模板编码获取表单数据结构（当前已发布版本的 FormSchemaField[]）
     *
     * @param code 模板编码
     * @return 表单数据结构（FormSchemaField[] 数组）
     */
    JsonNode getSchemaByCode(String code);

    /**
     * 按版本ID获取表单数据结构（任意版本，供历史版本结构预览）
     *
     * @param versionId 版本ID
     * @return 表单数据结构（FormSchemaField[] 数组）
     */
    JsonNode getSchemaByVersion(Long versionId);

    /**
     * 创建表单模板（自动创建版本 1 草稿）
     *
     * @param cmd 创建命令
     * @return 创建后的表单模板
     */
    FormTemplateVO create(FormTemplateCreateCmd cmd);

    /**
     * 更新表单模板主数据（部分更新：null 字段不修改）
     *
     * @param id  模板ID
     * @param cmd 更新命令
     * @return 更新后的表单模板
     */
    FormTemplateVO update(Long id, FormTemplateUpdateCmd cmd);

    /**
     * 删除表单模板及其全部版本
     *
     * @param id 模板ID
     */
    void delete(Long id);

    /**
     * 查询表单模板版本列表
     *
     * @param formTemplateId 模板ID
     * @return 版本列表
     */
    List<FormTemplateVersionVO> versions(Long formTemplateId);

    /**
     * 获取表单模板版本详情（不含表单定义）
     *
     * @param templateId 模板id
     * @param version 版本
     * @return 版本
     */
    FormTemplateVersionVO getVersion(Long templateId, Integer version);

    /**
     * 获取表单模板版本详情（含表单定义）
     *
     * @param versionId 版本Id
     * @return 版本
     */
    FormTemplateVersionVO getVersion(Long versionId);

    /**
     * 保存表单模板草稿（复用现有草稿或递增新版本；id 为 0 时自动创建模板主数据）
     *
     * @param formTemplateId 模板ID（新建时为 0）
     * @param cmd            保存草稿命令
     * @return 保存后的版本
     */
    FormTemplateVersionVO saveDraft(Long formTemplateId, FormTemplateSaveDraftCmd cmd);

    /**
     * 保存并发布表单模板草稿（先保存草稿，再原子发布为当前生效版本；id 为 0 时自动创建模板主数据）
     *
     * @param formTemplateId 模板ID（新建时为 0）
     * @param cmd            保存草稿命令
     * @return 发布后的版本
     */
    FormTemplateVersionVO saveAndPublish(Long formTemplateId, FormTemplateSaveDraftCmd cmd);

    /**
     * 上线表单模板版本（草稿或已下线 → 已发布，成为当前生效版本；仅已发布版本可下线）
     *
     * @param versionId 版本ID
     * @return 上线后的版本
     */
    FormTemplateVersionVO publish(Long versionId);

    /**
     * 下线表单模板版本（已发布 → 已下线）
     *
     * @param versionId 版本ID
     * @return 下线后的版本
     */
    FormTemplateVersionVO offline(Long versionId);
}