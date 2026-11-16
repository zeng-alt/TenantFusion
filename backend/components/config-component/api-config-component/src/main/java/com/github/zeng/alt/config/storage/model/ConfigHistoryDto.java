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
public record ConfigHistoryDto(
    Long historyId,
    Long configId,
    String content,
    Integer version,
    String operation,
    String operator,
    LocalDateTime createdDate
) {
    public Long getId() {
        return historyId;
    }
}
