package com.github.zeng.alt.oss;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分片上传信息。
 * <p>
 * 记录多部分上传中每个分片的上传结果，用于断点续传场景。
 * 客户端可通过查询已上传的分片列表，仅重新上传未完成的分片。
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
public class UploadPartInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 上传会话 ID（由 S3 CreateMultipartUpload 返回） */
    private String uploadId;

    /** 分片编号（从 1 开始） */
    private int partNumber;

    /** 分片 ETag（由 S3 UploadPart 返回，用于 complete 时组装） */
    private String etag;

    /** 分片大小（字节） */
    private Long size;

    /** 分片最后修改时间 */
    private LocalDateTime lastModified;

    public UploadPartInfo() {
    }

    public UploadPartInfo(String uploadId, int partNumber, String etag) {
        this.uploadId = uploadId;
        this.partNumber = partNumber;
        this.etag = etag;
    }

    public UploadPartInfo(String uploadId, int partNumber, String etag, Long size) {
        this.uploadId = uploadId;
        this.partNumber = partNumber;
        this.etag = etag;
        this.size = size;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(int partNumber) {
        this.partNumber = partNumber;
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

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public String toString() {
        return "UploadPartInfo{" +
                "uploadId='" + uploadId + '\'' +
                ", partNumber=" + partNumber +
                ", etag='" + etag + '\'' +
                ", size=" + size +
                '}';
    }
}
