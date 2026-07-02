package com.github.zeng.alt.oss;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * OSS 文件信息。
 * <p>
 * 封装文件上传/查询操作返回的元数据。
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
public class OssFileInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 文件名（完整路径，含目录） */
    private String fileName;

    /** 原始文件名 */
    private String originalFileName;

    /** 文件访问 URL */
    private String url;

    /** 文件 ETag */
    private String etag;

    /** 文件大小（字节） */
    private Long size;

    /** 内容类型 */
    private String contentType;

    /** 存储桶 */
    private String bucketName;

    /** 最后修改时间 */
    private LocalDateTime lastModified;

    /** 文件 MD5 哈希（用于去重校验） */
    private String md5;

    public OssFileInfo() {
    }

    public OssFileInfo(String fileName, String url, Long size) {
        this.fileName = fileName;
        this.url = url;
        this.size = size;
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
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

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    @Override
    public String toString() {
        return "OssFileInfo{" +
                "fileName='" + fileName + '\'' +
                ", originalFileName='" + originalFileName + '\'' +
                ", size=" + size +
                ", contentType='" + contentType + '\'' +
                ", bucketName='" + bucketName + '\'' +
                '}';
    }
}
