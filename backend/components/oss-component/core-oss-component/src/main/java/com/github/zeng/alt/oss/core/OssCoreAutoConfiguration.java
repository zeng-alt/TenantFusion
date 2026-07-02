package com.github.zeng.alt.oss.core;

import com.github.zeng.alt.oss.*;
import com.github.zeng.alt.oss.core.aot.OssRuntimeHints;
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
 * 当 {@code oss.s3.enabled=true}（默认）时，创建 {@link DefaultOssConnectionManager}，
 * 其内部维护一个可动态刷新的 {@link RefreshableOssTemplate}。
 * <p>
 * 同时注册以下可选组件：
 * <ul>
 *   <li>{@link DefaultBucketStrategy} — 自动桶策略（按文件类型分桶 + 按日期分路径）</li>
 *   <li>{@link S3MultipartUploadService} — 分片上传与断点续传</li>
 *   <li>{@link DefaultThumbnailService} — 图片缩略图生成</li>
 * </ul>
 *
 * <b>配置刷新集成</b>（Spring Cloud Config / Nacos / Apollo）：
 * <ol>
 *   <li>配置中心更新 {@code oss.s3.*} 属性后，Spring 发布 {@code EnvironmentChangeEvent}</li>
 *   <li>{@link DefaultOssConnectionManager} 监听该事件并自动触发刷新</li>
 *   <li>刷新时，管理器通过 {@link ObjectProvider} 获取最新的 {@link OssProperties}</li>
 *   <li>创建新连接 → 原子性切换 → 异步关闭旧连接</li>
 * </ol>
 * 也可通过 {@code POST /api/oss/refresh}（启用 {@code oss.s3.management.enabled=true}）手动触发。
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 2.0
 */
@AutoConfiguration
@ConditionalOnClass(S3Client.class)
@ConditionalOnProperty(prefix = "oss.s3", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({OssProperties.class, ThumbnailProperties.class})
@ImportRuntimeHints(OssRuntimeHints.class)
public class OssCoreAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(OssCoreAutoConfiguration.class);

    /**
     * 创建 OSS 连接管理器（单例）。
     */
    @Bean
    @ConditionalOnMissingBean
    public DefaultOssConnectionManager ossConnectionManager(ObjectProvider<OssProperties> propertiesProvider) {
        OssProperties props = propertiesProvider.getIfAvailable();
        if (props == null) {
            props = new OssProperties();
        }
        final OssProperties initialProps = props;

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
                        ensureBucketExists(s3Template.getS3Client(), initialProps.getBucketName());
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to auto-create OSS bucket '{}': {}", initialProps.getBucketName(), e.getMessage());
            }
        }

        return manager;
    }

    /**
     * 暴露 {@link OssTemplate} 给业务代码使用。
     */
    @Bean
    @ConditionalOnMissingBean
    public OssTemplate ossTemplate(OssConnectionManager connectionManager) {
        return connectionManager.getTemplate();
    }

    // ==================== 自动桶策略 ====================

    /**
     * 默认桶策略。
     * <p>
     * 仅在 {@code oss.s3.bucket-strategy-enabled=true} 时注册。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "oss.s3", name = "bucket-strategy-enabled", havingValue = "true")
    public BucketStrategy bucketStrategy(OssProperties properties) {
        log.info("OSS bucket strategy enabled: prefix={}, datePath={}",
                properties.getBucketPrefix(), properties.isDatePathEnabled());
        return new DefaultBucketStrategy(properties);
    }

    // ==================== 分片上传 / 断点续传 ====================

    /**
     * 基于 S3 的多部分上传服务。
     * <p>
     * 需要获取底层 S3Client。从 {@link OssConnectionManager} 管理的
     * {@link RefreshableOssTemplate} 中提取。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "oss.s3.upload", name = "enabled", havingValue = "true", matchIfMissing = true)
    public S3MultipartUploadService multipartUploadService(
            OssConnectionManager connectionManager,
            OssProperties properties) {
        S3Client s3Client = resolveS3Client(connectionManager);
        if (s3Client == null) {
            throw new IllegalStateException(
                    "Cannot create S3MultipartUploadService: unable to resolve S3Client. "
                            + "Ensure OssConnectionManager is properly initialized.");
        }
        return new S3MultipartUploadService(s3Client, properties);
    }

    // ==================== 缩略图生成 ====================

    /**
     * 默认缩略图生成服务。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "oss.thumbnail", name = "enabled", havingValue = "true", matchIfMissing = true)
    public DefaultThumbnailService thumbnailService(ThumbnailProperties thumbnailProperties) {
        return new DefaultThumbnailService(thumbnailProperties);
    }

    // ==================== 内部方法 ====================

    /**
     * 确保 Bucket 存在，不存在则创建。
     */
    private void ensureBucketExists(S3Client client, String bucketName) {
        try {
            client.headBucket(b -> b.bucket(bucketName));
            log.debug("OSS bucket already exists: {}", bucketName);
        } catch (software.amazon.awssdk.services.s3.model.NoSuchBucketException e) {
            client.createBucket(b -> b.bucket(bucketName));
            log.info("OSS bucket created: {}", bucketName);
        }
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
