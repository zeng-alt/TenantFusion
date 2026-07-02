package com.github.zeng.alt.oss.core;

import com.github.zeng.alt.oss.OssConnectionManager;
import com.github.zeng.alt.oss.OssProperties;
import com.github.zeng.alt.oss.OssTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 默认 {@link OssConnectionManager} 实现。
 * <p>
 * 基于 {@link RefreshableOssTemplate} 实现动态连接切换。
 * 刷新时：创建新连接 → 原子性切换 → 异步关闭旧连接（等待正在进行请求完成）。
 * <p>
 * 配置刷新集成：
 * <ul>
 *   <li>通过 {@link Supplier}<{@link OssProperties}> 保证每次刷新都使用最新配置</li>
 *   <li>{@link com.github.zeng.alt.oss.core.config.OssConfigRefreshAutoConfiguration} 监听
 *       {@code EnvironmentChangeEvent}（Spring Cloud Context），自动触发 OSS 刷新</li>
 *   <li>REST 端点 {@code POST /api/oss/refresh}（启用 {@code oss.s3.management.enabled=true}）手动触发</li>
 * </ul>
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 2.0
 */
public class DefaultOssConnectionManager implements OssConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(DefaultOssConnectionManager.class);

    private final Supplier<OssProperties> propertiesSupplier;
    private final RefreshableOssTemplate refreshableTemplate;

    /**
     * 创建连接管理器。
     *
     * @param propertiesSupplier 配置提供者，每次刷新时会调用以获取最新配置
     * @param initialProperties  初始配置（用于创建第一个连接）
     */
    public DefaultOssConnectionManager(Supplier<OssProperties> propertiesSupplier, OssProperties initialProperties) {
        this.propertiesSupplier = propertiesSupplier;
        S3OssTemplate initial = createS3Template(initialProperties);
        this.refreshableTemplate = new RefreshableOssTemplate(initial);
        log.info("OSS connection initialized: endpoint={}, bucket={}",
                initialProperties.getEndpoint(), initialProperties.getBucketName());
    }

    @Override
    public OssTemplate getTemplate() {
        return refreshableTemplate;
    }

    @Override
    public void refresh() {
        OssProperties currentProps = propertiesSupplier.get();
        if (currentProps == null || !StringUtils.hasText(currentProps.getEndpoint())) {
            log.warn("OSS refresh skipped: properties not available or endpoint is empty");
            return;
        }

        log.info("Refreshing OSS connection... new endpoint={}, bucket={}",
                currentProps.getEndpoint(), currentProps.getBucketName());

        // 1. 根据当前最新配置创建新连接
        S3OssTemplate newTemplate = createS3Template(currentProps);

        // 2. 原子性切换：新请求立即走新连接，旧连接引用被释放
        OssTemplate oldTemplate = refreshableTemplate.swapDelegate(newTemplate);

        // 3. 异步关闭旧连接（不阻塞 refresh 调用）
        //    S3Client.close() 会阻塞等待正在进行的请求完成后再释放资源
        if (oldTemplate instanceof S3OssTemplate s3Old) {
            CompletableFuture.runAsync(() -> {
                log.info("Gracefully shutting down old OSS connection...");
                s3Old.destroy();
                log.info("Old OSS connection shut down complete");
            });
        }
    }

    @Override
    public void destroy() {
        OssTemplate delegate = refreshableTemplate.getDelegate();
        if (delegate instanceof S3OssTemplate s3Template) {
            s3Template.destroy();
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 根据配置创建 {@link S3OssTemplate} 实例。
     */
    private S3OssTemplate createS3Template(OssProperties props) {
        S3Client s3Client = buildS3Client(props);
        S3Presigner s3Presigner = buildS3Presigner(props);
        return new S3OssTemplate(s3Client, s3Presigner, props);
    }

    /**
     * 构建 S3 客户端。
     */
    static S3Client buildS3Client(OssProperties properties) {
        var builder = S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(properties.isPathStyleAccess())
                        .build())
                .overrideConfiguration(c -> c
                        .apiCallAttemptTimeout(Duration.ofMillis(properties.getReadTimeout()))
                        .apiCallTimeout(Duration.ofMillis(properties.getConnectionTimeout() + properties.getReadTimeout())));

        if (properties.getEndpoint() != null) {
            builder = builder.endpointOverride(URI.create(properties.getEndpoint()));
        }

        return builder.build();
    }

    /**
     * 构建 S3 预签名 URL 生成器。
     */
    static S3Presigner buildS3Presigner(OssProperties properties) {
        var s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(properties.isPathStyleAccess())
                .build();
        return S3Presigner.builder()
                .region(Region.of(properties.getRegion()))
                .endpointOverride(URI.create(properties.getEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())
                ))
                .serviceConfiguration(s3Config)
                .build();
    }
}
