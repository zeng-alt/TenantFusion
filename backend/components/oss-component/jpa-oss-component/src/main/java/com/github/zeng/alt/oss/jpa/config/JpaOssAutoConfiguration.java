package com.github.zeng.alt.oss.jpa.config;

import com.github.zeng.alt.oss.*;
import com.github.zeng.alt.oss.jpa.entity.OssFileEntity;
import com.github.zeng.alt.oss.jpa.repository.OssFileRepository;
import com.github.zeng.alt.oss.jpa.service.JpaOssFileRecordService;
import com.github.zeng.alt.oss.jpa.service.PersistingOssTemplate;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.function.Supplier;

/**
 * OSS JPA 模块自动配置。
 * <p>
 * 除提供 {@link JpaOssFileRecordService} 文件记录查询服务外，
 * 还将 {@link OssTemplate} 自动增强为 {@link PersistingOssTemplate}：
 * <ul>
 *   <li>上传前计算 MD5 → 同一用户已上传过相同文件则跳过 S3 存储</li>
 *   <li>上传文件 → 按桶策略分桶 + S3 存储 + 图片缩略图生成 + 自动写入记录</li>
 *   <li>删除文件 → S3 删除 + 自动标记记录为已删除</li>
 * </ul>
 * 对业务代码完全透明，只需注入 {@link OssTemplate} 即可。
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 3.1
 */
@CommonsLog
@AutoConfiguration
@AutoConfigureAfter(HibernateJpaAutoConfiguration.class)
@ConditionalOnClass(EntityManagerFactory.class)
@AutoConfigurationPackage(basePackageClasses = {OssFileEntity.class, OssFileRepository.class})
public class JpaOssAutoConfiguration {

    /**
     * JPA 文件记录服务。
     */
    @Bean
    @ConditionalOnMissingBean
    public JpaOssFileRecordService jpaOssFileRecordService(OssFileRepository repository) {
        return new JpaOssFileRecordService(repository);
    }

    /**
     * 当前用户 ID 提供者。
     * <p>
     * 当 {@code api-security-component} 在类路径时，从 {@code UserContextHolder} 获取用户名；
     * 否则返回 {@code null}（去重不按用户隔离，仅按 MD5 全局去重）。
     */
    @Bean
    @ConditionalOnMissingBean
    public Supplier<String> ossUserIdProvider() {
        try {
            Class<?> holderClass = Class.forName("com.github.zeng.alt.security.api.UserContextHolder");
            var method = holderClass.getMethod("getUsername");
            return () -> {
                try {
                    return (String) method.invoke(null);
                } catch (Exception e) {
                    return null;
                }
            };
        } catch (Exception e) {
            log.debug("UserContextHolder not available, OSS deduplication will not isolate by user");
            return () -> null;
        }
    }

    /**
     * 持久化增强的 {@link OssTemplate}（MD5 去重 + 桶策略 + 缩略图 + 自动记录）。
     * <p>
     * 标记为 {@link Primary @Primary}，替换默认的纯 S3 模板。
     * <p>
     * 自动集成可选的 {@link BucketStrategy} 和 {@link ThumbnailService}：
     * - 若容器中存在这些 Bean 则自动注入，否则使用空实现。
     * <p>
     * 仅在 {@link OssTemplate} Bean 存在时创建（依赖 S3 或本地文件系统模板）。
     */
    @Bean
    @Primary
    @ConditionalOnBean(OssTemplate.class)
    public OssTemplate persistingOssTemplate(
            OssTemplate delegate,
            OssFileRecordService recordService,
            Supplier<String> userIdProvider,
            ObjectProvider<BucketStrategy> bucketStrategyProvider,
            ObjectProvider<ThumbnailService> thumbnailServiceProvider,
            ThumbnailProperties thumbnailProperties) {

        BucketStrategy bucketStrategy = bucketStrategyProvider.getIfAvailable();
        ThumbnailService thumbnailService = thumbnailServiceProvider.getIfAvailable();

        if (bucketStrategy != null) {
            log.info("OSS bucket strategy enabled: PersistingOssTemplate will use multi-bucket routing");
        }
        if (thumbnailService != null) {
            log.info("OSS thumbnail service enabled: image thumbnails will be auto-generated");
        }

        return new PersistingOssTemplate(
                delegate, recordService, userIdProvider,
                bucketStrategy, thumbnailService, thumbnailProperties
        );
    }
}
