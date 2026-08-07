package com.github.zeng.alt.workflow.repository;

import com.github.zeng.alt.workflow.model.FormConfigVersionStatus;

import java.time.LocalDateTime;

/**
 * 配置表单版本列表投影（不含大字段，用于版本列表查询）
 *
 * @author zengAlt
 */
public interface FormConfigVersionListProjection {

    Long getVersionId();

    Long getFormConfigId();

    Integer getVersion();

    FormConfigVersionStatus getStatus();

    Boolean getCurrent();

    LocalDateTime getPublishedDate();

    String getPublishedBy();

    String getRemark();

    String getCreatedBy();

    LocalDateTime getCreatedDate();
}
