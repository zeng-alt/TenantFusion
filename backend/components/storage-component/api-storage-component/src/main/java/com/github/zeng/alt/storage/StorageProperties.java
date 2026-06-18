package com.github.zeng.alt.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @author zengJiaJun
 * @since 2026年06月17日
 * @version 1.0
 */
@Data
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {
    private Integer initialCapacity = 100;
    private Integer maximumSize=500;
    private Duration expireTime = Duration.ofMinutes(30);
    private Boolean recordStats = false;
    private Boolean allowNullValues = true;
}
