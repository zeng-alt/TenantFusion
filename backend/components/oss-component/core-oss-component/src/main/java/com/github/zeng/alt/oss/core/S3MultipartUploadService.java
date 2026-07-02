package com.github.zeng.alt.oss.core;

import com.github.zeng.alt.oss.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于 AWS S3 的多部分上传服务实现。
 * <p>
 * 使用 S3 Multipart Upload API 实现大文件分片上传与断点续传。
 * 上传中断后，客户端可调用 {@link #listParts(String)} 查询已上传的分片，
 * 仅重新上传缺失的分片，最后调用 {@link #completeUpload} 完成合并。
 * <p>
 * 线程安全：每个 uploadId 对应独立的 S3 上传会话，无共享状态。
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
public class S3MultipartUploadService implements MultipartUploadService {

    private static final Logger log = LoggerFactory.getLogger(S3MultipartUploadService.class);

    private final S3Client s3Client;
    private final OssProperties properties;

    public S3MultipartUploadService(S3Client s3Client, OssProperties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    // ==================== 初始化 ====================

    @Override
    public String initiateUpload(String originalFileName, String contentType, Long totalSize) {
        String bucketName = properties.getBucketName();
        String fileName = buildFullPath(originalFileName);

        CreateMultipartUploadRequest.Builder builder = CreateMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(fileName);

        if (StringUtils.hasText(contentType)) {
            builder.contentType(contentType);
        }
        if (totalSize != null) {
            // 部分 S3 兼容服务支持此属性
            try {
                builder.metadata(java.util.Map.of("total-size", totalSize.toString()));
            } catch (Exception e) {
                // 忽略不支持的元数据
            }
        }

        CreateMultipartUploadResponse response = s3Client.createMultipartUpload(builder.build());
        String uploadId = response.uploadId();

        log.info("Multipart upload initiated: bucket={}, key={}, uploadId={}, totalSize={}",
                bucketName, fileName, uploadId, totalSize);
        return uploadId;
    }

    // ==================== 分片上传 ====================

    @Override
    public UploadPartInfo uploadPart(String uploadId, int partNumber, long partSize, InputStream data) {
        String bucketName = properties.getBucketName();
        // 使用最后一个已列出的文件名作为 key（需从 uploadId 追溯）
        // 实际场景中，客户端应在初始化后自行记录 key，或通过查询接口获取
        // 此处采用通用方式：从 S3 的 listMultipartUploads 获取 key
        String fileName = resolveKeyByUploadId(uploadId);
        if (fileName == null) {
            throw new OssException("Cannot resolve file key for uploadId: " + uploadId
                    + ". Ensure the upload was initiated via this service.");
        }

        try {
            UploadPartRequest request = UploadPartRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .uploadId(uploadId)
                    .partNumber(partNumber)
                    .contentLength(partSize)
                    .build();

            UploadPartResponse response = s3Client.uploadPart(
                    request, RequestBody.fromInputStream(data, partSize));

            UploadPartInfo info = new UploadPartInfo(uploadId, partNumber, response.eTag(), partSize);
            log.debug("Part uploaded: uploadId={}, partNumber={}, etag={}", uploadId, partNumber, response.eTag());
            return info;
        } catch (S3Exception e) {
            throw new OssException("Failed to upload part " + partNumber + " for upload " + uploadId, e);
        }
    }

    // ==================== 完成 / 取消 ====================

    @Override
    public OssFileInfo completeUpload(String uploadId, String bucketName, String fileName,
                                      List<UploadPartInfo> completedParts) {
        String fullPath = buildFullPath(fileName);

        // 如果未传入已上传分片列表，则自动从 S3 查询
        List<UploadPartInfo> parts = completedParts;
        if (parts == null || parts.isEmpty()) {
            parts = listParts(uploadId);
        }

        List<CompletedPart> completedSdkParts = parts.stream()
                .map(p -> CompletedPart.builder()
                        .partNumber(p.getPartNumber())
                        .eTag(p.getEtag())
                        .build())
                .collect(Collectors.toList());

        CompleteMultipartUploadRequest request = CompleteMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(fullPath)
                .uploadId(uploadId)
                .multipartUpload(CompletedMultipartUpload.builder()
                        .parts(completedSdkParts)
                        .build())
                .build();

        CompleteMultipartUploadResponse response = s3Client.completeMultipartUpload(request);

        log.info("Multipart upload completed: bucket={}, key={}, uploadId={}, etag={}",
                bucketName, fullPath, uploadId, response.eTag());

        OssFileInfo info = new OssFileInfo();
        info.setFileName(stripBasePath(fullPath));
        info.setEtag(response.eTag());
        info.setBucketName(bucketName);
        info.setUrl(buildObjectUrl(bucketName, fullPath));
        info.setSize(parts.stream().mapToLong(UploadPartInfo::getSize).sum());
        return info;
    }

    @Override
    public void abortUpload(String uploadId) {
        // 需要找到 uploadId 对应的 bucket 和 key
        String key = resolveKeyByUploadId(uploadId);
        if (key == null) {
            log.warn("Cannot abort upload {}: upload not found or already completed", uploadId);
            return;
        }
        String bucketName = properties.getBucketName();

        try {
            s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .uploadId(uploadId)
                    .build());
            log.info("Multipart upload aborted: bucket={}, key={}, uploadId={}", bucketName, key, uploadId);
        } catch (S3Exception e) {
            throw new OssException("Failed to abort multipart upload: " + uploadId, e);
        }
    }

    // ==================== 查询 ====================

    @Override
    public List<UploadPartInfo> listParts(String uploadId) {
        String key = resolveKeyByUploadId(uploadId);
        if (key == null) {
            return List.of();
        }
        String bucketName = properties.getBucketName();

        List<UploadPartInfo> result = new ArrayList<>();
        Integer partNumberMarker = null;
        boolean isTruncated;
        try {
            do {
                ListPartsRequest.Builder builder = ListPartsRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .uploadId(uploadId);
                if (partNumberMarker != null) {
                    builder.partNumberMarker(partNumberMarker);
                }
                ListPartsResponse response = s3Client.listParts(builder.build());
                for (Part part : response.parts()) {
                    UploadPartInfo info = new UploadPartInfo(
                            uploadId, part.partNumber(), part.eTag(), part.size());
                    if (part.lastModified() != null) {
                        info.setLastModified(LocalDateTime.ofInstant(
                                part.lastModified(), ZoneId.systemDefault()));
                    }
                    result.add(info);
                }
                // AWS SDK v2: use the last part number as marker for pagination
                isTruncated = Boolean.TRUE.equals(response.isTruncated());
                if (isTruncated && !response.parts().isEmpty()) {
                    partNumberMarker = response.parts().getLast().partNumber();
                } else {
                    partNumberMarker = null;
                }
            } while (isTruncated);
            log.debug("Listed parts for uploadId={}: count={}", uploadId, result.size());
            return result;
        } catch (S3Exception e) {
            throw new OssException("Failed to list parts for upload: " + uploadId, e);
        }
    }

    @Override
    public UploadSessionInfo getUploadStatus(String uploadId) {
        try {
            // 通过查询已上传分片来推断上传状态
            List<UploadPartInfo> parts = listParts(uploadId);
            if (parts.isEmpty()) {
                // 可能还未上传任何分片，或 uploadId 无效
                UploadSessionInfo info = new UploadSessionInfo();
                info.setUploadId(uploadId);
                info.setUploadStatus("INITIATED");
                info.setUploadedSize(0L);
                return info;
            }
            UploadSessionInfo info = new UploadSessionInfo();
            info.setUploadId(uploadId);
            info.setUploadStatus("IN_PROGRESS");
            info.setUploadedSize(parts.stream().mapToLong(UploadPartInfo::getSize).sum());
            return info;
        } catch (Exception e) {
            log.warn("Failed to get upload status for uploadId={}: {}", uploadId, e.getMessage());
            return null;
        }
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 通过 uploadId 查找对应的 S3 对象 key。
     * <p>
     * 遍历当前桶中所有正在进行的分片上传任务，匹配 uploadId。
     * 如果上传已完成或已取消，则返回 {@code null}。
     */
    private String resolveKeyByUploadId(String uploadId) {
        String bucketName = properties.getBucketName();
        String keyMarker = null;
        String uploadIdMarker = null;
        try {
            do {
                ListMultipartUploadsRequest.Builder builder = ListMultipartUploadsRequest.builder()
                        .bucket(bucketName);
                if (keyMarker != null) {
                    builder.keyMarker(keyMarker);
                }
                if (uploadIdMarker != null) {
                    builder.uploadIdMarker(uploadIdMarker);
                }
                ListMultipartUploadsResponse response = s3Client.listMultipartUploads(builder.build());
                for (MultipartUpload upload : response.uploads()) {
                    if (upload.uploadId().equals(uploadId)) {
                        return upload.key();
                    }
                }
                keyMarker = response.nextKeyMarker();
                uploadIdMarker = response.nextUploadIdMarker();
            } while (keyMarker != null);
            return null;
        } catch (S3Exception e) {
            log.warn("Error resolving key for uploadId={}: {}", uploadId, e.getMessage());
            return null;
        }
    }

    /**
     * 拼接完整路径（basePath + fileName）。
     */
    private String buildFullPath(String fileName) {
        String basePath = properties.getBasePath();
        if (!StringUtils.hasText(basePath)) {
            return fileName;
        }
        String normalizedBase = basePath.endsWith("/") ? basePath : basePath + "/";
        return normalizedBase + fileName;
    }

    /**
     * 移除 basePath 前缀。
     */
    private String stripBasePath(String fullPath) {
        String basePath = properties.getBasePath();
        if (!StringUtils.hasText(basePath) || !fullPath.startsWith(basePath)) {
            return fullPath;
        }
        String stripped = fullPath.substring(basePath.length());
        return stripped.startsWith("/") ? stripped.substring(1) : stripped;
    }

    /**
     * 构建对象访问 URL。
     */
    private String buildObjectUrl(String bucketName, String fullPath) {
        String endpoint = properties.getEndpoint();
        if (endpoint == null) {
            return null;
        }
        String base = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        if (properties.isPathStyleAccess()) {
            return base + "/" + bucketName + "/" + fullPath;
        } else {
            return base.replace("://", "://" + bucketName + ".") + "/" + fullPath;
        }
    }
}
