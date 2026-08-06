package com.github.zeng.alt.workflow.repository;

import com.github.zeng.alt.workflow.model.FormTemplateVersionStatus;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 表单模板版本列表投影。
 * <p>
 * 用于版本列表查询：只加载展示所需的列，不加载体积庞大的
 * {@code definition}（CLOB），避免列表接口产生不必要的内存与 IO 开销。
 *
 * @author zengAlt
 */
public interface FormTemplateVersionListProjection {

    Long getVersionId();

    Long getFormTemplateId();

    Integer getVersion();

    FormTemplateVersionStatus getStatus();

    Boolean getCurrent();

    LocalDateTime getPublishedDate();

    String getPublishedBy();

    String getRemark();

    Optional<String> getCreatedBy();

    Optional<LocalDateTime> getCreatedDate();
}
