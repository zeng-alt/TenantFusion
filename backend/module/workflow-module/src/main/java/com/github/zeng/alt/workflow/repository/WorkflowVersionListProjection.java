package com.github.zeng.alt.workflow.repository;

import com.github.zeng.alt.workflow.model.FormTemplateVersionStatus;
import com.github.zeng.alt.workflow.model.WorkflowVersionStatus;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * @author zengJiaJun
 * @since 2026年08月06日
 * @version 1.0
 */
public interface WorkflowVersionListProjection {

    Long getVersionId();

    Long getWorkflowId();

    Integer getVersion();

    WorkflowVersionStatus getStatus();

    Boolean getCurrent();

    LocalDateTime getPublishedDate();

    String getPublishedBy();

    String getRemark();

    Optional<String> getCreatedBy();

    Optional<LocalDateTime> getCreatedDate();
}

