package com.github.zeng.alt.oss;

/**
 * 对象存储类型。
 * <p>
 * 区分底层存储实现方式，用于自动选择对应的 {@link OssTemplate} 实现。
 * S3 兼容类型（MINIO / AWS_S3 / ALIYUN_OSS / TENCENT_COS / HUAWEI_OBS）
 * 共享同一套 S3 协议实现，仅 endpoint 和配置不同；
 * FILE 类型使用本地文件系统实现。
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
public enum StorageType {

    /** 本地文件系统，endpoint 格式：file:///d:/data/oss */
    FILE("file"),

    /** MinIO */
    MINIO("minio"),

    /** AWS S3 */
    AWS_S3("aws"),

    /** 阿里云 OSS */
    ALIYUN_OSS("aliyun"),

    /** 腾讯云 COS */
    TENCENT_COS("tencent"),

    /** 华为云 OBS */
    HUAWEI_OBS("huawei");

    private final String value;

    StorageType(String value) {
        this.value = value;
    }

    /**
     * 获取配置值（用于配置文件中的 {@code oss.s3.storage-type}）。
     */
    public String getValue() {
        return value;
    }

    /**
     * 判断是否为 S3 协议兼容类型。
     */
    public boolean isS3Compatible() {
        return this != FILE;
    }

    /**
     * 根据配置值解析枚举。
     *
     * @param value 配置值，如 {@code file}、{@code minio}、{@code aws} 等
     * @return 对应的枚举，无法识别时返回 {@code MINIO}
     */
    public static StorageType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return MINIO;
        }
        String lower = value.trim().toLowerCase();
        for (StorageType type : values()) {
            if (type.value.equals(lower)) {
                return type;
            }
        }
        // 兼容旧配置：直接匹配枚举名
        try {
            return valueOf(lower.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MINIO;
        }
    }
}
