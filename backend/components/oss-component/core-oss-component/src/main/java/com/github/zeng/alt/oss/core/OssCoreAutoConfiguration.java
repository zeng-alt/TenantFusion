package com.github.zeng.alt.oss.core;

import com.github.zeng.alt.oss.*;
import com.github.zeng.alt.oss.core.aot.OssRuntimeHints;
import com.github.zeng.alt.oss.core.local.FileSystemOssTemplate;
import com.github.zeng.alt.oss.core.s3.S3MultipartUploadService;
import com.github.zeng.alt.oss.core.s3.S3OssTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * OSS 核心自动配置。
 * <p>
 * 根据 {@link StorageType} 自动选择底层存储实现：
 * <ul>
 *   <li><b>S3 兼容类型</b>（MINIO / AWS_S3 / ALIYUN_OSS / TENCENT_COS / HUAWEI_OBS）：
 *       使用 {@link DefaultOssConnectionManager} + {@link S3OssTemplate}，基于 AWS S3 协议</li>
 *   <li><b>本地文件系统</b>（FILE）：
 *       使用 {@link FileSystemOssTemplate}，基于 {@code file://} 协议</li>
 * </ul>
 * <p>
 * 同时注册以下可选组件：
 * <ul>
 *   <li>{@link DefaultBucketStrategy} — 自动桶策略（按文件类型分桶 + 按日期分路径）</li>
 *   <li>{@link S3MultipartUploadService} — 分片上传与断点续传（仅 S3 类型）</li>
 *   <li>{@link DefaultThumbnailService} — 图片缩略图生成</li>
 * </ul>
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 3.0
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "oss.s3", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({OssProperties.class, ThumbnailProperties.class})
@ImportRuntimeHints(OssRuntimeHints.class)
public class OssCoreAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(OssCoreAutoConfiguration.class);

    // ==================== OssTemplate ====================

    /**
     * 根据存储类型创建 {@link OssTemplate}。
     * <p>
     * - {@link StorageType#FILE}: 本地文件系统 {@link FileSystemOssTemplate}
     * - S3 兼容类型: {@link DefaultOssConnectionManager}（包装 {@link S3OssTemplate}）
     */
    @Bean
    @ConditionalOnMissingBean
    public OssTemplate ossTemplate(ObjectProvider<OssProperties> propertiesProvider) {
        OssProperties props = propertiesProvider.getIfAvailable();
        if (props == null) {
            props = new OssProperties();
        }

        StorageType storageType = props.getStorageType();
        log.info("Initializing OSS with storage type: {}, endpoint: {}", storageType, props.getEndpoint());

        if (storageType == StorageType.FILE) {
            // ===== 本地文件系统 =====
            return createFileSystemTemplate(props);
        }

        // ===== S3 兼容类型 (MINIO / AWS_S3 / ALIYUN_OSS / TENCENT_COS / HUAWEI_OBS) =====
        if (!isS3ClientAvailable()) {
            throw new IllegalStateException(
                    "S3 storage type '" + storageType + "' requires S3Client on classpath, "
                            + "but software.amazon.awssdk.services.s3.S3Client is not available. "
                            + "Add the AWS S3 SDK dependency or use storage-type=file.");
        }
        return createS3Template(props, propertiesProvider);
    }

    // ==================== 自动桶策略 ====================

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "oss.s3", name = "bucket-strategy-enabled", havingValue = "true")
    public BucketStrategy bucketStrategy(OssProperties properties) {
        log.info("OSS bucket strategy enabled: prefix={}, datePath={}",
                properties.getBucketPrefix(), properties.isDatePathEnabled());
        return new DefaultBucketStrategy(properties);
    }

    // ==================== 分片上传 / 断点续传（仅 S3） ====================

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(S3Client.class)
    @ConditionalOnProperty(prefix = "oss.s3.upload", name = "enabled", havingValue = "true", matchIfMissing = true)
    public MultipartUploadService multipartUploadService(
            ObjectProvider<OssConnectionManager> connectionManagerProvider,
            OssProperties properties) {
        OssConnectionManager connectionManager = connectionManagerProvider.getIfAvailable();
        if (connectionManager == null) {
            log.warn("Cannot create S3MultipartUploadService: OssConnectionManager not available");
            return null;
        }
        S3Client s3Client = resolveS3Client(connectionManager);
        if (s3Client == null) {
            throw new IllegalStateException(
                    "Cannot create S3MultipartUploadService: unable to resolve S3Client. "
                            + "Ensure OssConnectionManager is properly initialized.");
        }
        return new S3MultipartUploadService(s3Client, properties);
    }

    // ==================== 缩略图生成 ====================

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "oss.thumbnail", name = "enabled", havingValue = "true", matchIfMissing = true)
    public DefaultThumbnailService thumbnailService(ThumbnailProperties thumbnailProperties) {
        return new DefaultThumbnailService(thumbnailProperties);
    }

    // ==================== 内部方法 ====================

    /**
     * 创建 S3 兼容模板（含连接管理器）。
     */
    private OssTemplate createS3Template(OssProperties initialProps,
                                          ObjectProvider<OssProperties> propertiesProvider) {
        DefaultOssConnectionManager manager = new DefaultOssConnectionManager(
                () -> {
                    OssProperties current = propertiesProvider.getIfAvailable();
                    return current != null ? current : initialProps;
                },
                initialProps
        );

        // 启动时自动创建默认 Bucket
        if (initialProps.isAutoCreateBucket() && initialProps.getBucketName() != null) {
            try {
                OssTemplate template = manager.getTemplate();
                if (template instanceof RefreshableOssTemplate refreshable) {
                    if (refreshable.getDelegate() instanceof S3OssTemplate s3Template) {
                        ensureS3BucketExists(s3Template, initialProps.getBucketName());
                    }
                } else if (template instanceof S3OssTemplate s3Template) {
                    ensureS3BucketExists(s3Template, initialProps.getBucketName());
                }
            } catch (Exception e) {
                log.warn("Failed to auto-create OSS bucket '{}': {}", initialProps.getBucketName(), e.getMessage());
            }
        }

        return manager.getTemplate();
    }

    /**
     * 创建本地文件系统模板。
     */
    private OssTemplate createFileSystemTemplate(OssProperties props) {
        return new FileSystemOssTemplate(props);
    }

    /**
     * 检查 S3Client 是否在类路径中。
     */
    private static boolean isS3ClientAvailable() {
        try {
            Class.forName("software.amazon.awssdk.services.s3.S3Client");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * 确保 S3 Bucket 存在。
     */
    private void ensureS3BucketExists(S3OssTemplate template, String bucketName) {
        template.ensureBucketExists(bucketName);
    }

    /**
     * 从连接管理器中解析底层 S3Client。
     */
    private static S3Client resolveS3Client(OssConnectionManager connectionManager) {
        OssTemplate template = connectionManager.getTemplate();
        if (template instanceof RefreshableOssTemplate refreshable) {
            if (refreshable.getDelegate() instanceof S3OssTemplate s3Template) {
                return s3Template.getS3Client();
            }
        }
        if (template instanceof S3OssTemplate s3Template) {
            return s3Template.getS3Client();
        }
        return null;
    }
}
