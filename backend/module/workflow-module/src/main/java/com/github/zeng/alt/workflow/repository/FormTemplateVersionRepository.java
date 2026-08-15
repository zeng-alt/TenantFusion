package com.github.zeng.alt.workflow.repository;

import com.github.zeng.alt.domain.base.BaseRepository;
import com.github.zeng.alt.workflow.entity.FormTemplateVersionEntity;
import com.github.zeng.alt.workflow.model.FormTemplateVersionStatus;
import io.vavr.control.Option;

import java.util.List;
import java.util.Optional;

/**
 * 表单模板版本表 Repository
 *
 * @author zengAlt
 */
public interface FormTemplateVersionRepository extends BaseRepository<FormTemplateVersionEntity, Long> {


    Option<FormTemplateVersionListProjection> findByFormTemplateIdAndVersion(Long formTemplateId, Integer version);

    /**
     * 查询表单模板指定版本号
     *
     * @param formTemplateId 表单模板ID
     * @param version        版本号
     * @return 版本记录
     */
    Optional<FormTemplateVersionEntity> findFirstByFormTemplateIdAndVersion(Long formTemplateId, Integer version);

    /**
     * 查询表单模板的全部版本，按版本号倒序
     *
     * @param formTemplateId 表单模板ID
     * @return 版本列表
     */
    List<FormTemplateVersionEntity> findByFormTemplateIdOrderByVersionDesc(Long formTemplateId);

    /**
     * 查询表单模板的全部版本（投影：不加载 definition 大字段），按版本号倒序
     *
     * @param formTemplateId 表单模板ID
     * @return 版本列表投影
     */
    List<FormTemplateVersionListProjection> findProjectionByFormTemplateIdOrderByVersionDesc(Long formTemplateId);

    /**
     * 查询表单模板当前生效版本
     *
     * @param formTemplateId 表单模板ID
     * @return 版本记录
     */
    Optional<FormTemplateVersionEntity> findFirstByFormTemplateIdAndCurrentTrue(Long formTemplateId);

    /**
     * 查询表单模板中草稿版本（最多一条）
     *
     * @param formTemplateId 表单模板ID
     * @param status         状态
     * @return 版本记录
     */
    Optional<FormTemplateVersionEntity> findFirstByFormTemplateIdAndStatusOrderByVersionDesc(Long formTemplateId, FormTemplateVersionStatus status);

    /**
     * 查询表单模板中指定状态的版本列表
     *
     * @param formTemplateId 表单模板ID
     * @param status         状态
     * @return 版本列表
     */
    List<FormTemplateVersionEntity> findByFormTemplateIdAndStatus(Long formTemplateId, FormTemplateVersionStatus status);

    /**
     * 删除表单模板下的全部版本
     *
     * @param formTemplateId 表单模板ID
     */
    void deleteByFormTemplateId(Long formTemplateId);
}
