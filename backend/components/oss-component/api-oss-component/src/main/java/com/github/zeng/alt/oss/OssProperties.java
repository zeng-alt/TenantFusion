package com.github.zeng.alt.oss;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OSS 配置属性。
 * <p>
 * 前缀：{@code oss.s3}
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
@ConfigurationProperties(prefix = "oss.s3")
public class OssProperties {

    /** 是否启用 */
    private boolean enabled = false;

    /** 对象存储服务的 URL 或者 file:///d:/data/oss */
    private String endpoint;

    /** 存储类型，可选：file / minio / aws / aliyun / tencent / huawei，默认 minio */
    private StorageType storageType = StorageType.MINIO;

    /** 区域 */
    private String region = "us-east-1";

    /** Access Key */
    private String accessKey;

    /** Secret Key */
    private String secretKey;

    /** 默认存储桶名称 */
    private String bucketName;

    /** 基础路径（可选，所有文件上传时自动拼接此前缀） */
    private String basePath = "";

    /**
     * 路径风格访问。
     * <p>
     * 当未显式设置时，根据 {@link #storageType} 自动推导：
     * <ul>
     *   <li>{@code MINIO} → {@code true}（路径风格）</li>
     *   <li>{@code AWS_S3 / ALIYUN_OSS / TENCENT_COS / HUAWEI_OBS} → {@code false}（虚拟主机风格）</li>
     *   <li>{@code FILE} → 不适用</li>
     * </ul>
     * 显式设置此值将覆盖自动推导。
     */
    private Boolean pathStyleAccess;

    /** 最大上传文件大小（字节），默认 100MB */
    private long maxUploadSize = 104_857_600;

    /** 预签名 URL 默认过期时间（秒），默认 600 */
    private int presignedUrlExpiration = 600;

    /** 连接超时（毫秒） */
    private long connectionTimeout = 5000;

    /** 读取超时（毫秒） */
    private long readTimeout = 30000;

    /** 上传分片大小（字节），默认 5MB */
    private int multipartPartSize = 5_242_880;

    /** 是否在启动时自动创建默认 Bucket */
    private boolean autoCreateBucket = true;

    // ==================== 桶策略 ====================

    /** 是否启用自动桶策略（按文件类型自动分配桶），默认 false */
    private boolean bucketStrategyEnabled = false;

    /** 桶名前缀（自动建桶时使用），默认 {@code app} */
    private String bucketPrefix = "app";

    /** 是否在路径中使用 {@code /年/月/} 日期划分，默认 true */
    private boolean datePathEnabled = true;

    /** 桶名后缀映射（覆盖默认的文件类型后缀），例如 {@code image: pictures, document: files} */
    private java.util.Map<String, String> bucketSuffixOverride = new java.util.HashMap<>();

    // ==================== 管理端点 ====================

    /** 是否启用管理端点（{@code POST /api/oss/refresh}），默认 false */
    private boolean managementEnabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public StorageType getStorageType() {
        return storageType;
    }

    public void setStorageType(StorageType storageType) {
        this.storageType = storageType;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public boolean isPathStyleAccess() {
        if (pathStyleAccess != null) {
            return pathStyleAccess;
        }
        // 未显式设置时，根据存储类型自动推导
        return storageType == StorageType.MINIO;
    }

    /**
     * 获取原始的 pathStyleAccess 设置值（可能为 null）。
     */
    public Boolean getPathStyleAccessRaw() {
        return pathStyleAccess;
    }

    public void setPathStyleAccess(Boolean pathStyleAccess) {
        this.pathStyleAccess = pathStyleAccess;
    }

    public long getMaxUploadSize() {
        return maxUploadSize;
    }

    public void setMaxUploadSize(long maxUploadSize) {
        this.maxUploadSize = maxUploadSize;
    }

    public int getPresignedUrlExpiration() {
        return presignedUrlExpiration;
    }

    public void setPresignedUrlExpiration(int presignedUrlExpiration) {
        this.presignedUrlExpiration = presignedUrlExpiration;
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public long getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(long readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getMultipartPartSize() {
        return multipartPartSize;
    }

    public void setMultipartPartSize(int multipartPartSize) {
        this.multipartPartSize = multipartPartSize;
    }

    public boolean isAutoCreateBucket() {
        return autoCreateBucket;
    }

    public void setAutoCreateBucket(boolean autoCreateBucket) {
        this.autoCreateBucket = autoCreateBucket;
    }

    public boolean isBucketStrategyEnabled() {
        return bucketStrategyEnabled;
    }

    public void setBucketStrategyEnabled(boolean bucketStrategyEnabled) {
        this.bucketStrategyEnabled = bucketStrategyEnabled;
    }

    public String getBucketPrefix() {
        return bucketPrefix;
    }

    public void setBucketPrefix(String bucketPrefix) {
        this.bucketPrefix = bucketPrefix;
    }

    public boolean isDatePathEnabled() {
        return datePathEnabled;
    }

    public void setDatePathEnabled(boolean datePathEnabled) {
        this.datePathEnabled = datePathEnabled;
    }

    public java.util.Map<String, String> getBucketSuffixOverride() {
        return bucketSuffixOverride;
    }

    public void setBucketSuffixOverride(java.util.Map<String, String> bucketSuffixOverride) {
        this.bucketSuffixOverride = bucketSuffixOverride;
    }

    public boolean isManagementEnabled() {
        return managementEnabled;
    }

    public void setManagementEnabled(boolean managementEnabled) {
        this.managementEnabled = managementEnabled;
    }
}
