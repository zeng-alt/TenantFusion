package com.github.zeng.alt.oss.jpa.service;

import com.github.zeng.alt.oss.*;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.core.log.LogMessage;
import org.springframework.util.StringUtils;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;


/**
 * 持久化增强的 {@link OssTemplate} 装饰器。
 * <p>
 * 在真实的 {@link OssTemplate} 操作之上自动叠加文件记录持久化、MD5 去重、
 * 自动桶策略（按类型分桶 + 按日期分路径）和图片缩略图生成能力：
 * <ul>
 *   <li>上传前计算文件 MD5，检查同一用户是否已上传过相同文件</li>
 *   <li>已存在 → 直接返回已有文件记录（跳过 S3 上传，防止重复存储与计费）</li>
 *   <li>不存在 → 按桶策略确定桶和路径 → S3 上传 + 生成缩略图 + 自动写入记录</li>
 *   <li>删除 → S3 删除 + 自动标记数据库记录为已删除</li>
 * </ul>
 * <p>
 * 当引入 {@code jpa-oss-component} 时，由 {@code JpaOssAutoConfiguration} 自动注册为
 * 主要的 {@link OssTemplate} Bean，对业务代码透明。
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 3.0
 */
@CommonsLog
public class PersistingOssTemplate implements OssTemplate {

    private final OssTemplate delegate;
    private final OssFileRecordService recordService;
    private final UserIdProvider userIdProvider;
    private final BucketStrategy bucketStrategy;
    private final ThumbnailService thumbnailService;
    private final ThumbnailProperties thumbnailProperties;

    /**
     * 完整构造函数。
     *
     * @param delegate            底层 S3 操作模板
     * @param recordService       文件记录持久化服务
     * @param userIdProvider      当前用户 ID 提供者
     * @param bucketStrategy      桶策略（可为 {@code null}）
     * @param thumbnailService    缩略图生成服务（可为 {@code null}）
     * @param thumbnailProperties 缩略图配置
     */
    public PersistingOssTemplate(OssTemplate delegate, OssFileRecordService recordService,
                                  UserIdProvider userIdProvider,
                                  BucketStrategy bucketStrategy,
                                  ThumbnailService thumbnailService,
                                  ThumbnailProperties thumbnailProperties) {
        this.delegate = delegate;
        this.recordService = recordService;
        this.userIdProvider = userIdProvider;
        this.bucketStrategy = bucketStrategy;
        this.thumbnailService = thumbnailService;
        this.thumbnailProperties = thumbnailProperties;
    }

    // ==================== 上传（MD5 去重 + 桶策略 + 缩略图） ====================

    @Override
    public OssFileInfo upload(InputStream inputStream, String fileName) {
        return upload(inputStream, fileName, null);
    }

    @Override
    public OssFileInfo upload(InputStream inputStream, String fileName, String contentType) {
        try {
            byte[] data = inputStream.readAllBytes();
            return uploadWithEnhancements(data, fileName, contentType);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read input stream", e);
        }
    }

    @Override
    public OssFileInfo upload(byte[] data, String fileName) {
        return uploadWithEnhancements(data, fileName, null);
    }

    @Override
    public OssFileInfo upload(byte[] data, String fileName, String contentType) {
        return uploadWithEnhancements(data, fileName, contentType);
    }

    @Override
    public OssFileInfo upload(File file, String fileName) {
        try (InputStream is = new FileInputStream(file)) {
            byte[] data = is.readAllBytes();
            OssFileInfo info = uploadWithEnhancements(data, fileName, null);
            if (info.getSize() == null) {
                info.setSize(file.length());
            }
            return info;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read file", e);
        }
    }

    /**
     * 上传核心：MD5 去重 → 桶策略 → S3 上传 → 缩略图生成 → 保存记录。
     */
    private OssFileInfo uploadWithEnhancements(byte[] data, String fileName, String contentType) {
        String md5 = computeMd5(data);
        String userId = userIdProvider.getUserId();

        // 去重检查
        if (userId != null) {
            OssFileInfo existing = recordService.findExistingByMd5(md5, userId);
            if (existing != null) {
                log.info(LogMessage.format("Duplicate file detected (md5=%s), returning existing record: %s", md5, existing.getFileName()));
                return existing;
            }
        }

        // 文件类型检测
        String originalName = extractOriginalName(fileName);
        FileType fileType = FileType.detect(originalName, contentType);
        String detectedContentType = contentType;
        if (detectedContentType == null) {
            detectedContentType = detectMimeType(originalName, fileType);
        }

        // 桶策略：确定桶名和路径
        String targetBucket;
        String storageFileName;
        if (bucketStrategy != null) {
            targetBucket = bucketStrategy.determineBucketName(originalName, detectedContentType, fileType);
            String pathPrefix = bucketStrategy.determinePathPrefix(originalName, fileType);
            storageFileName = pathPrefix + fileName;
        } else {
            targetBucket = null; // 使用默认桶
            storageFileName = fileName;
        }

        // 上传到 OSS
        OssFileInfo info;
        if (targetBucket != null) {
            info = delegate.upload(targetBucket, data, storageFileName, detectedContentType);
            // 确保目标桶存在（首次上传时自动创建）
            try {
                delegate.ensureBucketExists(targetBucket);
            } catch (Exception e) {
                log.debug(LogMessage.format("Bucket check/setup for '%s': %s", targetBucket, e.getMessage()));
            }
        } else {
            if (detectedContentType != null) {
                info = delegate.upload(data, storageFileName, detectedContentType);
            } else {
                info = delegate.upload(data, storageFileName);
            }
        }

        info.setMd5(md5);
        info.setOriginalFileName(originalName);

        // 缩略图生成（仅图片类型）
        OssFileInfo thumbInfo = null;
        if (thumbnailService != null && thumbnailProperties != null && thumbnailProperties.isEnabled()
                && fileType.isImage() && data.length > 0) {
            try {
                thumbInfo = thumbnailService.generateThumbnail(data, storageFileName,
                        detectedContentType, delegate, bucketStrategy);
            } catch (Exception e) {
                log.warn(LogMessage.format("Failed to generate thumbnail for %s: %s", storageFileName, e.getMessage()));
            }
        }

        // 保存文件记录
        recordService.saveRecord(info, originalName);

        return info;
    }

    // ==================== 下载（透传） ====================

    @Override
    public InputStream download(String fileName) {
        return delegate.download(fileName);
    }

    // ==================== 删除（自动标记记录） ====================

    @Override
    public void delete(String fileName) {
        delegate.delete(fileName);
        recordService.markDeletedByFileName(fileName);
    }

    @Override
    public void delete(List<String> fileNames) {
        delegate.delete(fileNames);
        for (String fileName : fileNames) {
            recordService.markDeletedByFileName(fileName);
        }
    }

    // ==================== 查询（透传） ====================

    @Override
    public boolean exists(String fileName) {
        return delegate.exists(fileName);
    }

    @Override
    public OssFileInfo getFileInfo(String fileName) {
        return delegate.getFileInfo(fileName);
    }

    @Override
    public String getUrl(String fileName) {
        return delegate.getUrl(fileName);
    }

    @Override
    public List<OssFileInfo> listFiles(String prefix) {
        return delegate.listFiles(prefix);
    }

    // ==================== 其他操作 ====================

    @Override
    public void copy(String sourceFileName, String targetFileName) {
        delegate.copy(sourceFileName, targetFileName);
    }

    @Override
    public void move(String sourceFileName, String targetFileName) {
        delegate.move(sourceFileName, targetFileName);
    }

    @Override
    public String presignedGetUrl(String fileName, int expiration) {
        return delegate.presignedGetUrl(fileName, expiration);
    }

    // ==================== 桶感知操作 ====================

    @Override
    public OssFileInfo upload(String bucketName, InputStream inputStream, String fileName) {
        return delegate.upload(bucketName, inputStream, fileName);
    }

    @Override
    public OssFileInfo upload(String bucketName, InputStream inputStream, String fileName, String contentType) {
        return delegate.upload(bucketName, inputStream, fileName, contentType);
    }

    @Override
    public OssFileInfo upload(String bucketName, byte[] data, String fileName) {
        return delegate.upload(bucketName, data, fileName);
    }

    @Override
    public OssFileInfo upload(String bucketName, byte[] data, String fileName, String contentType) {
        return delegate.upload(bucketName, data, fileName, contentType);
    }

    @Override
    public boolean exists(String bucketName, String fileName) {
        return delegate.exists(bucketName, fileName);
    }

    @Override
    public void delete(String bucketName, String fileName) {
        delegate.delete(bucketName, fileName);
    }

    @Override
    public void ensureBucketExists(String bucketName) {
        delegate.ensureBucketExists(bucketName);
    }

    // ==================== 内部方法 ====================

    /**
     * 计算字节数组的 MD5 哈希（小写十六进制）。
     */
    private String computeMd5(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    /**
     * 从完整路径中提取原始文件名（最后一段）。
     */
    private String extractOriginalName(String fileName) {
        if (fileName == null) return "";
        int idx = fileName.lastIndexOf('/');
        return idx >= 0 ? fileName.substring(idx + 1) : fileName;
    }

    /**
     * 根据文件名和文件类型推测 MIME 类型。
     */
    private String detectMimeType(String fileName, FileType fileType) {
        if (!StringUtils.hasText(fileName)) return null;
        String lower = fileName.toLowerCase();
        return switch (fileType) {
            case IMAGE -> {
                if (lower.endsWith(".png")) yield "image/png";
                if (lower.endsWith(".gif")) yield "image/gif";
                if (lower.endsWith(".webp")) yield "image/webp";
                if (lower.endsWith(".bmp")) yield "image/bmp";
                if (lower.endsWith(".svg")) yield "image/svg+xml";
                if (lower.endsWith(".ico")) yield "image/x-icon";
                yield "image/jpeg";
            }
            case DOCUMENT -> {
                if (lower.endsWith(".pdf")) yield "application/pdf";
                if (lower.endsWith(".doc") || lower.endsWith(".docx")) yield "application/msword";
                if (lower.endsWith(".xls") || lower.endsWith(".xlsx")) yield "application/vnd.ms-excel";
                if (lower.endsWith(".ppt") || lower.endsWith(".pptx"))
                    yield "application/vnd.ms-powerpoint";
                if (lower.endsWith(".txt")) yield "text/plain";
                if (lower.endsWith(".csv")) yield "text/csv";
                yield "application/octet-stream";
            }
            case ARCHIVE -> {
                if (lower.endsWith(".zip")) yield "application/zip";
                if (lower.endsWith(".rar")) yield "application/vnd.rar";
                if (lower.endsWith(".tar")) yield "application/x-tar";
                if (lower.endsWith(".gz")) yield "application/gzip";
                yield "application/octet-stream";
            }
            case AUDIO -> {
                if (lower.endsWith(".mp3")) yield "audio/mpeg";
                if (lower.endsWith(".wav")) yield "audio/wav";
                if (lower.endsWith(".ogg")) yield "audio/ogg";
                if (lower.endsWith(".flac")) yield "audio/flac";
                if (lower.endsWith(".aac")) yield "audio/aac";
                yield "audio/mpeg";
            }
            case VIDEO -> {
                if (lower.endsWith(".mp4")) yield "video/mp4";
                if (lower.endsWith(".avi")) yield "video/x-msvideo";
                if (lower.endsWith(".mkv")) yield "video/x-matroska";
                if (lower.endsWith(".webm")) yield "video/webm";
                if (lower.endsWith(".mov")) yield "video/quicktime";
                yield "video/mp4";
            }
            default -> null;
        };
    }
}
