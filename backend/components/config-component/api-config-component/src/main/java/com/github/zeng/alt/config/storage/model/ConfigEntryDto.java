package com.github.zeng.alt.config.storage.model;

import java.time.LocalDateTime;
import lombok.Builder;

/**
 * 与具体存储技术无关的配置持久化模型。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Builder(toBuilder = true)
public record ConfigEntryDto(
    Long configId,
    Long appId,
    String dataId,
    String group,
    String content,
    String format,
    String description,
    Integer version,
    String status,
    String tenantId,
    String createdBy,
    LocalDateTime createdDate,
    String lastModifiedBy,
    LocalDateTime lastModifiedDate
) {
    public Long getId() {
        return configId;
    }
}
