package com.github.zeng.alt.oss.jpa.entity;

import com.github.zeng.alt.domain.base.BaseEntity;
import jakarta.persistence.*;
import java.io.Serial;

/**
 * OSS 文件记录实体。
 * <p>
 * 映射 {@code sys_oss_file} 表，记录上传到对象存储的文件元数据。
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
@Entity
@Table(name = "sys_oss_file")
public class OssFileEntity extends BaseEntity<Long> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long fileId;

    /** 文件名（存储路径，含目录） */
    @Column(name = "file_name", nullable = false)
    private String fileName;

    /** 原始文件名 */
    @Column(name = "original_file_name")
    private String originalFileName;

    /** 文件后缀 */
    @Column(name = "file_suffix")
    private String fileSuffix;

    /** 文件大小（字节） */
    @Column(name = "file_size")
    private Long fileSize;

    /** 内容类型 */
    @Column(name = "content_type")
    private String contentType;

    /** 存储桶 */
    @Column(name = "bucket_name")
    private String bucketName;

    /** 文件 ETag */
    @Column(name = "etag")
    private String etag;

    /** 文件 MD5 哈希（用于去重校验） */
    @Column(name = "md5", length = 32)
    private String md5;

    /** 访问 URL */
    @Column(name = "url", length = 2048)
    private String url;

    /** 存储类型（s3 / minio / cos / oss 等） */
    @Column(name = "storage_type")
    private String storageType;

    /** 文件类型分类（IMAGE / DOCUMENT / ARCHIVE / AUDIO / VIDEO / OTHER） */
    @Column(name = "file_type")
    private String fileType;

    /** 缩略图文件名（含路径） */
    @Column(name = "thumbnail_name")
    private String thumbnailName;

    /** 缩略图访问 URL */
    @Column(name = "thumbnail_url", length = 2048)
    private String thumbnailUrl;

    /** 缩略图宽度（像素） */
    @Column(name = "thumbnail_width")
    private Integer thumbnailWidth;

    /** 缩略图高度（像素） */
    @Column(name = "thumbnail_height")
    private Integer thumbnailHeight;

    /** 上传会话 ID（用于分片上传追踪） */
    @Column(name = "upload_id")
    private String uploadId;

    /** 分片上传状态（INITIATED / IN_PROGRESS / COMPLETED / ABORTED） */
    @Column(name = "upload_status", length = 20)
    private String uploadStatus;

    /** 状态：0-正常，1-已删除 */
    @Column(name = "status")
    private Integer status;

    /** 租户 ID */
    @Column(name = "tenant_id")
    private String tenantId;

    public OssFileEntity() {
    }

    @Override
    public Long getId() {
        return fileId;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getFileSuffix() {
        return fileSuffix;
    }

    public void setFileSuffix(String fileSuffix) {
        this.fileSuffix = fileSuffix;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getThumbnailName() {
        return thumbnailName;
    }

    public void setThumbnailName(String thumbnailName) {
        this.thumbnailName = thumbnailName;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public Integer getThumbnailWidth() {
        return thumbnailWidth;
    }

    public void setThumbnailWidth(Integer thumbnailWidth) {
        this.thumbnailWidth = thumbnailWidth;
    }

    public Integer getThumbnailHeight() {
        return thumbnailHeight;
    }

    public void setThumbnailHeight(Integer thumbnailHeight) {
        this.thumbnailHeight = thumbnailHeight;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public String getUploadStatus() {
        return uploadStatus;
    }

    public void setUploadStatus(String uploadStatus) {
        this.uploadStatus = uploadStatus;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
