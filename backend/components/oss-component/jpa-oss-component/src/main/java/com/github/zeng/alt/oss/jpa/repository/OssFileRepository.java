package com.github.zeng.alt.oss.jpa.repository;

import com.github.zeng.alt.domain.base.BaseRepository;
import com.github.zeng.alt.oss.jpa.entity.OssFileEntity;
import com.github.zeng.alt.rest.annotation.CrudRest;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OSS 文件记录 Repository。
 * <p>
 * 标注 {@link CrudRest @CrudRest} 后，在配置了 {@code rest-apt-component}
 * 注解处理器的模块中编译时会自动生成 CRUD REST 接口。
 * 同时，{@code jpa-oss-component} 内置的 {@code OssFileController}
 * 提供同等功能的运行时 REST 接口，无需 APT 处理器。
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
@CrudRest(path = "/oss-files")
public interface OssFileRepository extends BaseRepository<OssFileEntity, Long> {

    /**
     * 根据文件名查询
     *
     * @param fileName 文件名
     * @return 文件记录
     */
    List<OssFileEntity> findByFileName(String fileName);

    /**
     * 根据原始文件名模糊查询
     *
     * @param originalFileName 原始文件名
     * @return 文件记录列表
     */
    List<OssFileEntity> findByOriginalFileNameContaining(String originalFileName);

    /**
     * 根据 Bucket 查询文件列表
     *
     * @param bucketName 存储桶
     * @return 文件记录列表
     */
    List<OssFileEntity> findByBucketName(String bucketName);

    /**
     * 根据状态查询
     *
     * @param status 状态
     * @return 文件记录列表
     */
    List<OssFileEntity> findByStatus(Integer status);

    /**
     * 查询指定时间之前创建的文件
     *
     * @param time 时间
     * @return 文件记录列表
     */
    List<OssFileEntity> findByCreatedDateBefore(LocalDateTime time);

    /**
     * 查询指定存储类型的文件列表
     *
     * @param storageType 存储类型
     * @return 文件记录列表
     */
    List<OssFileEntity> findByStorageType(String storageType);

    /**
     * 根据 MD5 和创建人查询有效文件记录（用于去重校验）
     *
     * @param md5        文件 MD5
     * @param createdBy  创建人
     * @param status     状态（0-正常）
     * @return 文件记录，不存在返回空列表
     */
    List<OssFileEntity> findByMd5AndCreatedByAndStatus(String md5, String createdBy, Integer status);

    /**
     * 根据上传会话 ID 查询文件记录
     *
     * @param uploadId 上传会话 ID
     * @return 文件记录
     */
    List<OssFileEntity> findByUploadId(String uploadId);

    /**
     * 根据文件类型分类查询
     *
     * @param fileType 文件类型
     * @return 文件记录列表
     */
    List<OssFileEntity> findByFileType(String fileType);

    /**
     * 查询包含缩略图的文件记录
     *
     * @return 文件记录列表
     */
    List<OssFileEntity> findByThumbnailNameIsNotNull();

    /**
     * 查询未完成的分片上传记录
     *
     * @param uploadStatus 上传状态
     * @return 文件记录列表
     */
    List<OssFileEntity> findByUploadStatus(String uploadStatus);
}
