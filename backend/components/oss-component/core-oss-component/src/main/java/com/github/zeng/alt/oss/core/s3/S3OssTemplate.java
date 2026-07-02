package com.github.zeng.alt.oss.core.s3;

import com.github.zeng.alt.oss.OssFileInfo;
import com.github.zeng.alt.oss.OssProperties;
import com.github.zeng.alt.oss.OssTemplate;
import com.github.zeng.alt.oss.core.OssException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AWS S3 协议 {@link OssTemplate} 实现。
 * <p>
 * 兼容 AWS S3、MinIO、腾讯云 COS、阿里云 OSS、华为云 OBS 等所有支持 S3 协议的对象存储服务。
 * <p>
 * 根据 {@link StorageType} 自动选择：MINIO / AWS_S3 / ALIYUN_OSS / TENCENT_COS / HUAWEI_OBS。
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
public class S3OssTemplate implements OssTemplate {

    private static final Logger log = LoggerFactory.getLogger(S3OssTemplate.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final OssProperties properties;

    public S3OssTemplate(S3Client s3Client, S3Presigner s3Presigner, OssProperties properties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
    }

    @Override
    public OssFileInfo upload(InputStream inputStream, String fileName) {
        return upload(inputStream, fileName, null);
    }

    @Override
    public OssFileInfo upload(InputStream inputStream, String fileName, String contentType) {
        String fullPath = buildFullPath(fileName);
        String bucketName = properties.getBucketName();
        try {
            PutObjectRequest.Builder builder = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fullPath);
            if (StringUtils.hasText(contentType)) {
                builder.contentType(contentType);
            }
            PutObjectResponse response = s3Client.putObject(
                    builder.build(),
                    RequestBody.fromInputStream(inputStream, -1)
            );
            log.debug("OSS upload success: bucket={}, key={}, etag={}", bucketName, fullPath, response.eTag());
            return buildFileInfo(fullPath, contentType, response);
        } catch (S3Exception e) {
            throw new OssException("OSS upload failed: " + fullPath, e);
        }
    }

    @Override
    public OssFileInfo upload(byte[] data, String fileName) {
        return upload(data, fileName, null);
    }

    @Override
    public OssFileInfo upload(byte[] data, String fileName, String contentType) {
        String fullPath = buildFullPath(fileName);
        String bucketName = properties.getBucketName();
        try {
            PutObjectRequest.Builder builder = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fullPath);
            if (StringUtils.hasText(contentType)) {
                builder.contentType(contentType);
            }
            PutObjectResponse response = s3Client.putObject(
                    builder.build(),
                    RequestBody.fromBytes(data)
            );
            log.debug("OSS upload success: bucket={}, key={}, etag={}", bucketName, fullPath, response.eTag());
            return buildFileInfo(fullPath, contentType, response);
        } catch (S3Exception e) {
            throw new OssException("OSS upload failed: " + fullPath, e);
        }
    }

    @Override
    public OssFileInfo upload(File file, String fileName) {
        String fullPath = buildFullPath(fileName);
        String bucketName = properties.getBucketName();
        try {
            PutObjectResponse response = s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(fullPath)
                            .build(),
                    RequestBody.fromFile(file.toPath())
            );
            log.debug("OSS upload success: bucket={}, key={}, etag={}", bucketName, fullPath, response.eTag());
            OssFileInfo info = buildFileInfo(fullPath, null, response);
            info.setSize(file.length());
            return info;
        } catch (S3Exception e) {
            throw new OssException("OSS upload failed: " + fullPath, e);
        }
    }

    @Override
    public InputStream download(String fileName) {
        String fullPath = buildFullPath(fileName);
        try {
            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(properties.getBucketName())
                            .key(fullPath)
                            .build()
            );
            return response;
        } catch (NoSuchKeyException e) {
            return null;
        } catch (S3Exception e) {
            throw new OssException("OSS download failed: " + fullPath, e);
        }
    }

    @Override
    public void delete(String fileName) {
        String fullPath = buildFullPath(fileName);
        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(properties.getBucketName())
                            .key(fullPath)
                            .build()
            );
            log.debug("OSS delete success: bucket={}, key={}", properties.getBucketName(), fullPath);
        } catch (S3Exception e) {
            throw new OssException("OSS delete failed: " + fullPath, e);
        }
    }

    @Override
    public void delete(List<String> fileNames) {
        List<ObjectIdentifier> keys = fileNames.stream()
                .map(f -> ObjectIdentifier.builder().key(buildFullPath(f)).build())
                .collect(Collectors.toList());
        try {
            s3Client.deleteObjects(
                    DeleteObjectsRequest.builder()
                            .bucket(properties.getBucketName())
                            .delete(Delete.builder().objects(keys).build())
                            .build()
            );
            log.debug("OSS batch delete success: bucket={}, count={}", properties.getBucketName(), keys.size());
        } catch (S3Exception e) {
            throw new OssException("OSS batch delete failed", e);
        }
    }

    @Override
    public boolean exists(String fileName) {
        String fullPath = buildFullPath(fileName);
        try {
            s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(properties.getBucketName())
                            .key(fullPath)
                            .build()
            );
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            return false;
        }
    }

    @Override
    public OssFileInfo getFileInfo(String fileName) {
        String fullPath = buildFullPath(fileName);
        try {
            HeadObjectResponse response = s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(properties.getBucketName())
                            .key(fullPath)
                            .build()
            );
            return buildFileInfo(fullPath, response);
        } catch (NoSuchKeyException e) {
            return null;
        } catch (S3Exception e) {
            throw new OssException("OSS getFileInfo failed: " + fullPath, e);
        }
    }

    @Override
    public String getUrl(String fileName) {
        String fullPath = buildFullPath(fileName);
        if (!exists(fileName)) {
            return null;
        }
        return buildObjectUrl(fullPath);
    }

    @Override
    public List<OssFileInfo> listFiles(String prefix) {
        String fullPrefix = buildFullPath(prefix);
        List<OssFileInfo> result = new ArrayList<>();
        try {
            ListObjectsV2Response response;
            String continuationToken = null;
            do {
                ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder()
                        .bucket(properties.getBucketName())
                        .prefix(fullPrefix);
                if (continuationToken != null) {
                    builder.continuationToken(continuationToken);
                }
                response = s3Client.listObjectsV2(builder.build());
                for (S3Object s3Object : response.contents()) {
                    OssFileInfo info = new OssFileInfo();
                    info.setFileName(stripBasePath(s3Object.key()));
                    info.setSize(s3Object.size());
                    info.setEtag(s3Object.eTag());
                    info.setLastModified(s3Object.lastModified() != null
                            ? LocalDateTime.ofInstant(s3Object.lastModified(), ZoneId.systemDefault())
                            : null);
                    info.setBucketName(properties.getBucketName());
                    info.setUrl(buildObjectUrl(s3Object.key()));
                    result.add(info);
                }
                continuationToken = response.nextContinuationToken();
            } while (response.isTruncated());
            return result;
        } catch (S3Exception e) {
            throw new OssException("OSS listFiles failed, prefix: " + fullPrefix, e);
        }
    }

    @Override
    public void copy(String sourceFileName, String targetFileName) {
        String sourceFullPath = buildFullPath(sourceFileName);
        String targetFullPath = buildFullPath(targetFileName);
        String bucketName = properties.getBucketName();
        try {
            s3Client.copyObject(
                    CopyObjectRequest.builder()
                            .sourceBucket(bucketName)
                            .sourceKey(sourceFullPath)
                            .destinationBucket(bucketName)
                            .destinationKey(targetFullPath)
                            .build()
            );
            log.debug("OSS copy success: {}/{} -> {}/{}", bucketName, sourceFullPath, bucketName, targetFullPath);
        } catch (S3Exception e) {
            throw new OssException("OSS copy failed: " + sourceFullPath + " -> " + targetFullPath, e);
        }
    }

    @Override
    public void move(String sourceFileName, String targetFileName) {
        copy(sourceFileName, targetFileName);
        delete(sourceFileName);
    }

    @Override
    public String presignedGetUrl(String fileName, int expiration) {
        String fullPath = buildFullPath(fileName);
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(expiration))
                    .getObjectRequest(GetObjectRequest.builder()
                            .bucket(properties.getBucketName())
                            .key(fullPath)
                            .build())
                    .build();
            URL url = s3Presigner.presignGetObject(presignRequest).url();
            return url.toString();
        } catch (S3Exception e) {
            throw new OssException("OSS presigned URL generation failed: " + fullPath, e);
        }
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 拼接完整路径（basePath + fileName）
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
     * 移除 basePath 前缀，返回原始 fileName
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
     * 构建对象访问 URL（使用默认桶）
     */
    private String buildObjectUrl(String fullPath) {
        return buildObjectUrl(properties.getBucketName(), fullPath);
    }

    /**
     * 构建对象访问 URL（指定桶名）
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

    /**
     * 从上传响应构建 OssFileInfo
     */
    private OssFileInfo buildFileInfo(String fullPath, String contentType, PutObjectResponse response) {
        return buildFileInfo(properties.getBucketName(), fullPath, contentType, response);
    }

    /**
     * 从上传响应构建 OssFileInfo（指定桶名）
     */
    private OssFileInfo buildFileInfo(String bucketName, String fullPath, String contentType, PutObjectResponse response) {
        OssFileInfo info = new OssFileInfo();
        info.setFileName(stripBasePath(fullPath));
        info.setEtag(response.eTag());
        info.setContentType(contentType);
        info.setBucketName(bucketName);
        info.setUrl(buildObjectUrl(bucketName, fullPath));
        return info;
    }

    /**
     * 从 HeadObject 响应构建 OssFileInfo
     */
    private OssFileInfo buildFileInfo(String fullPath, HeadObjectResponse response) {
        OssFileInfo info = new OssFileInfo();
        info.setFileName(stripBasePath(fullPath));
        info.setSize(response.contentLength());
        info.setContentType(response.contentType());
        info.setEtag(response.eTag());
        info.setBucketName(properties.getBucketName());
        info.setLastModified(response.lastModified() != null
                ? LocalDateTime.ofInstant(response.lastModified(), ZoneId.systemDefault())
                : null);
        info.setUrl(buildObjectUrl(fullPath));
        return info;
    }

    // ==================== 桶感知操作（用于自动桶策略） ====================

    @Override
    public OssFileInfo upload(String bucketName, InputStream inputStream, String fileName) {
        return upload(bucketName, inputStream, fileName, null);
    }

    @Override
    public OssFileInfo upload(String bucketName, InputStream inputStream, String fileName, String contentType) {
        String fullPath = buildFullPath(fileName);
        try {
            PutObjectRequest.Builder builder = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fullPath);
            if (StringUtils.hasText(contentType)) {
                builder.contentType(contentType);
            }
            PutObjectResponse response = s3Client.putObject(
                    builder.build(),
                    RequestBody.fromInputStream(inputStream, -1)
            );
            log.debug("OSS upload (bucket-aware) success: bucket={}, key={}, etag={}", bucketName, fullPath, response.eTag());
            return buildFileInfo(bucketName, fullPath, contentType, response);
        } catch (S3Exception e) {
            throw new OssException("OSS upload failed to bucket " + bucketName + ": " + fullPath, e);
        }
    }

    @Override
    public OssFileInfo upload(String bucketName, byte[] data, String fileName) {
        return upload(bucketName, data, fileName, null);
    }

    @Override
    public OssFileInfo upload(String bucketName, byte[] data, String fileName, String contentType) {
        String fullPath = buildFullPath(fileName);
        try {
            PutObjectRequest.Builder builder = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fullPath);
            if (StringUtils.hasText(contentType)) {
                builder.contentType(contentType);
            }
            PutObjectResponse response = s3Client.putObject(
                    builder.build(),
                    RequestBody.fromBytes(data)
            );
            log.debug("OSS upload (bucket-aware) success: bucket={}, key={}, etag={}", bucketName, fullPath, response.eTag());
            return buildFileInfo(bucketName, fullPath, contentType, response);
        } catch (S3Exception e) {
            throw new OssException("OSS upload failed to bucket " + bucketName + ": " + fullPath, e);
        }
    }

    @Override
    public boolean exists(String bucketName, String fileName) {
        String fullPath = buildFullPath(fileName);
        try {
            s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(bucketName)
                            .key(fullPath)
                            .build()
            );
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            return false;
        }
    }

    @Override
    public void delete(String bucketName, String fileName) {
        String fullPath = buildFullPath(fileName);
        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(fullPath)
                            .build()
            );
            log.debug("OSS delete (bucket-aware) success: bucket={}, key={}", bucketName, fullPath);
        } catch (S3Exception e) {
            throw new OssException("OSS delete failed from bucket " + bucketName + ": " + fullPath, e);
        }
    }

    @Override
    public void ensureBucketExists(String bucketName) {
        try {
            s3Client.headBucket(b -> b.bucket(bucketName));
            log.debug("OSS bucket already exists: {}", bucketName);
        } catch (software.amazon.awssdk.services.s3.model.NoSuchBucketException e) {
            s3Client.createBucket(b -> b.bucket(bucketName));
            log.info("OSS bucket created: {}", bucketName);
        } catch (S3Exception e) {
            log.warn("Failed to ensure bucket exists '{}': {}", bucketName, e.getMessage());
        }
    }

    // ==================== 生命周期管理 ====================

    /**
     * 获取底层 S3Client，用于管理操作。
     */
    public S3Client getS3Client() {
        return s3Client;
    }

    /**
     * 获取底层 S3Presigner。
     */
    public S3Presigner getS3Presigner() {
        return s3Presigner;
    }

    /**
     * 获取当前配置。
     */
    public OssProperties getProperties() {
        return properties;
    }

    /**
     * 优雅关闭底层客户端连接。
     */
    public void destroy() {
        log.info("Shutting down OSS connection: bucket={}, endpoint={}",
                properties.getBucketName(), properties.getEndpoint());
        try {
            s3Presigner.close();
        } catch (Exception e) {
            log.warn("Error closing S3Presigner", e);
        }
        try {
            s3Client.close();
        } catch (Exception e) {
            log.warn("Error closing S3Client", e);
        }
    }
}
