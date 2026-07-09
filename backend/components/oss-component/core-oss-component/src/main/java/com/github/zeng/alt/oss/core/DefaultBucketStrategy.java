package com.github.zeng.alt.oss.core;

import com.github.zeng.alt.oss.BucketStrategy;
import com.github.zeng.alt.oss.FileType;
import com.github.zeng.alt.oss.OssProperties;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.core.log.LogMessage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 默认桶策略实现。
 * <p>
 * - 桶名规则：{@code {prefix}-{type_suffix}}，如 {@code app-images}、{@code app-documents}
 * - 路径规则：{@code {year}/{month}/}，如 {@code 2026/07/}
 * - 后缀可在 {@link OssProperties#getBucketSuffixOverride()} 中自定义覆盖
 * <p>
 * 可通过 {@code oss.s3.bucket-strategy-enabled=true} 启用。
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
@CommonsLog
public class DefaultBucketStrategy implements BucketStrategy {

    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyy/MM");

    private final String bucketPrefix;
    private final boolean datePathEnabled;
    private final java.util.Map<String, String> suffixOverrides;

    /**
     * @param properties OSS 配置属性
     */
    public DefaultBucketStrategy(OssProperties properties) {
        this.bucketPrefix = properties.getBucketPrefix() != null ? properties.getBucketPrefix() : "app";
        this.datePathEnabled = properties.isDatePathEnabled();
        this.suffixOverrides = properties.getBucketSuffixOverride() != null
                ? properties.getBucketSuffixOverride()
                : java.util.Map.of();
    }

    @Override
    public String determineBucketName(String originalFileName, String contentType, FileType fileType) {
        String defaultSuffix = fileType.getBucketSuffix();
        // 优先使用自定义后缀映射
        String suffix = suffixOverrides.getOrDefault(fileType.name(), defaultSuffix);
        String bucketName = bucketPrefix + "-" + suffix;
        log.debug(LogMessage.format("Bucket determined: fileType=%s, bucket=%s", fileType, bucketName));
        return bucketName;
    }

    @Override
    public String determinePathPrefix(String originalFileName, FileType fileType) {
        if (!datePathEnabled) {
            return "";
        }
        LocalDate now = LocalDate.now();
        return now.format(YEAR_MONTH) + "/";
    }

    /**
     * 获取当前配置的桶前缀。
     */
    public String getBucketPrefix() {
        return bucketPrefix;
    }
}
