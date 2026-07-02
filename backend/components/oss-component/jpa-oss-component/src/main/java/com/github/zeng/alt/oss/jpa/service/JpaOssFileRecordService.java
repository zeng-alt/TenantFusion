package com.github.zeng.alt.oss.jpa.service;

import com.github.zeng.alt.oss.OssFileInfo;
import com.github.zeng.alt.oss.OssFileRecordService;
import com.github.zeng.alt.oss.jpa.entity.OssFileEntity;
import com.github.zeng.alt.oss.jpa.repository.OssFileRepository;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * OSS 文件记录 JPA 实现。
 * <p>
 * 实现 {@link OssFileRecordService} 接口，使用 Spring Data JPA 将文件元数据持久化到数据库。
 * 同时提供扩展查询方法，供 {@link com.github.zeng.alt.oss.jpa.controller.OssFileController} 使用。
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 2.0
 */
public class JpaOssFileRecordService implements OssFileRecordService {

    private static final Logger log = LoggerFactory.getLogger(JpaOssFileRecordService.class);

    private final OssFileRepository repository;

    public JpaOssFileRecordService(OssFileRepository repository) {
        this.repository = repository;
    }

    // ==================== OssFileRecordService 接口实现 ====================

    @Override
    public void saveRecord(OssFileInfo info, String originalFileName) {
        OssFileEntity entity = convert(info, originalFileName, "s3");
        entity.setStatus(0);
        repository.save(entity);
        log.debug("OSS file record saved: fileName={}, size={}", info.getFileName(), info.getSize());
    }

    @Override
    public void markDeletedByFileName(String fileName) {
        List<OssFileEntity> records = repository.findByFileName(fileName);
        for (OssFileEntity record : records) {
            record.setStatus(1);
            repository.save(record);
        }
        if (!records.isEmpty()) {
            log.debug("OSS file records marked as deleted: fileName={}, count={}", fileName, records.size());
        }
    }

    @Override
    public void cleanUp(LocalDateTime before) {
        List<OssFileEntity> records = repository.findByCreatedDateBefore(before);
        records.forEach(entity -> repository.deleteById(Objects.requireNonNull(entity.getId())));
        log.debug("Cleaned up {} OSS file records before {}", records.size(), before);
    }

    @Override
    public OssFileInfo findExistingByMd5(String md5, String userId) {
        if (md5 == null || userId == null) {
            return null;
        }
        List<OssFileEntity> records = repository.findByMd5AndCreatedByAndStatus(md5, userId, 0);
        if (records.isEmpty()) {
            return null;
        }
        OssFileEntity entity = records.get(0);
        OssFileInfo info = new OssFileInfo();
        info.setFileName(entity.getFileName());
        info.setOriginalFileName(entity.getOriginalFileName());
        info.setUrl(entity.getUrl());
        info.setEtag(entity.getEtag());
        info.setMd5(entity.getMd5());
        info.setSize(entity.getFileSize());
        info.setContentType(entity.getContentType());
        info.setBucketName(entity.getBucketName());
        info.setLastModified(entity.getCreatedDate().orElse(null));
        return info;
    }

    // ==================== 扩展查询方法 ====================

    /**
     * 根据 ID 查询文件记录。
     */
    public Option<OssFileEntity> getById(Long fileId) {
        return repository.findById(fileId);
    }

    /**
     * 根据文件名查询记录。
     */
    public List<OssFileEntity> getByFileName(String fileName) {
        return repository.findByFileName(fileName);
    }

    /**
     * 查询所有文件记录。
     */
    public List<OssFileEntity> listAll() {
        return repository.findAll();
    }

    /**
     * 物理删除文件记录。
     */
    public void deleteById(Long fileId) {
        repository.deleteById(fileId);
    }

    /**
     * 标记文件为已删除。
     */
    public void markDeleted(Long fileId) {
        repository.findById(fileId)
                .peek(entity -> {
                    entity.setStatus(1);
                    repository.save(entity);
                });
    }

    /**
     * 获取底层 Repository（供扩展使用）。
     */
    public OssFileRepository getRepository() {
        return repository;
    }

    // ==================== 内部方法 ====================

    /**
     * 将 {@link OssFileInfo} 转换为 {@link OssFileEntity}。
     */
    private OssFileEntity convert(OssFileInfo info, String originalFileName, String storageType) {
        OssFileEntity entity = new OssFileEntity();
        entity.setFileName(info.getFileName());
        entity.setOriginalFileName(originalFileName);
        entity.setFileSize(info.getSize());
        entity.setContentType(info.getContentType());
        entity.setBucketName(info.getBucketName());
        entity.setEtag(info.getEtag());
        entity.setMd5(info.getMd5());
        entity.setUrl(info.getUrl());

        // 提取文件后缀
        if (StringUtils.hasText(originalFileName)) {
            int dotIndex = originalFileName.lastIndexOf('.');
            if (dotIndex > 0) {
                entity.setFileSuffix(originalFileName.substring(dotIndex));
            }
        }

        entity.setStorageType(StringUtils.hasText(storageType) ? storageType : "s3");
        return entity;
    }
}
