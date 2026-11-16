package com.github.zeng.alt.config.client;

import com.github.zeng.alt.config.ConfigTemplate;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "config.client")
public class ConfigClientProperties {
    private boolean enabled = true;
    @NotBlank
    @Pattern(regexp = "https?://.+")
    private String serverAddr = "http://localhost:8085";
    @NotBlank
    @Pattern(regexp = "[a-zA-Z0-9][a-zA-Z0-9_.-]{0,63}")
    private String appCode;
    @Min(1000)
    @Max(60000)
    private long longPollTimeoutMs = 30000;
    private String cacheDir;
    @NotBlank
    private String defaultGroup = ConfigTemplate.DEFAULT_GROUP;
    @Min(1)
    private int connectTimeoutMs = 3000;
    @Min(1)
    private int requestTimeoutMs = 5000;
    @Min(1)
    private long retryDelayMs = 1000;
    @Min(1000)
    private long refreshIntervalMs = 30000;
    private boolean refreshEnabled = true;
    private boolean refreshScopeEnabled;
}
