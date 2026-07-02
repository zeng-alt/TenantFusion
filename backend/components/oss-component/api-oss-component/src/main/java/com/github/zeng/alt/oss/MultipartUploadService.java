package com.github.zeng.alt.oss;

import java.io.InputStream;
import java.util.List;

/**
 * 多部分上传服务（断点续传）。
 * <p>
 * 基于 AWS S3 Multipart Upload API 实现文件分片上传与断点续传能力。
 * 适用于大文件上传场景：将文件拆分为多个分片独立上传，
 * 传输中断后只需重新上传未完成的分片，无需重新开始整个上传任务。
 * <p>
 * 典型使用流程：
 * <ol>
 *   <li>{@link #initiateUpload(String, String, Long)} 初始化上传，获取 uploadId</li>
 *   <li>{@link #uploadPart(String, int, long, InputStream)} 逐片上传数据</li>
 *   <li>若上传中断，调用 {@link #listParts(String)} 查询已上传的分片</li>
 *   <li>重新上传缺失的分片后，调用 {@link #completeUpload(String, String, String)} 完成合并</li>
 *   <li>或调用 {@link #abortUpload(String)} 放弃上传</li>
 * </ol>
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
public interface MultipartUploadService {

    // ==================== 初始化 ====================

    /**
     * 初始化分片上传。
     *
     * @param originalFileName 原始文件名
     * @param contentType      MIME 类型，{@code null} 则自动探测
     * @param totalSize        文件总大小（字节），{@code null} 表示未知
     * @return 上传会话 ID（uploadId），用于后续分片上传、完成、取消等操作
     */
    String initiateUpload(String originalFileName, String contentType, Long totalSize);

    // ==================== 分片上传 ====================

    /**
     * 上传单个分片。
     *
     * @param uploadId   上传会话 ID
     * @param partNumber 分片编号（从 1 开始）
     * @param partSize   分片大小（字节）
     * @param data       分片数据流（调用方负责关闭）
     * @return 分片上传结果，包含 ETag
     */
    UploadPartInfo uploadPart(String uploadId, int partNumber, long partSize, InputStream data);

    // ==================== 完成 / 取消 ====================

    /**
     * 完成分片上传，合并所有已上传的分片为完整的文件。
     * <p>
     * 调用此方法前，必须已通过 {@link #uploadPart} 上传了所有分片。
     * 如果使用服务端分片管理，parts 参数可省略（由服务端自动组装所有已上传的分片）。
     *
     * @param uploadId         上传会话 ID
     * @param bucketName       存储桶名称
     * @param fileName         文件名（含路径）
     * @param completedParts   已上传的分片列表（含 etag）；{@code null} 时由服务端自动获取
     * @return 合并后的文件信息
     */
    OssFileInfo completeUpload(String uploadId, String bucketName, String fileName,
                               List<UploadPartInfo> completedParts);

    /**
     * 取消分片上传，清理已上传的分片数据。
     *
     * @param uploadId 上传会话 ID
     */
    void abortUpload(String uploadId);

    // ==================== 查询 ====================

    /**
     * 查询已上传的分片列表（用于断点续传）。
     * <p>
     * 客户端在上传中断后可调用此方法获取已成功上传的分片编号，
     * 仅重新上传缺失或上传失败的分片。
     *
     * @param uploadId 上传会话 ID
     * @return 已上传的分片信息列表
     */
    List<UploadPartInfo> listParts(String uploadId);

    /**
     * 查询上传会话状态。
     *
     * @param uploadId 上传会话 ID
     * @return 上传会话信息（uploadId, fileName, totalSize, uploadedSize 等），
     *         不存在返回 {@code null}
     */
    UploadSessionInfo getUploadStatus(String uploadId);

    // ==================== 内部类型 ====================

    /**
     * 上传会话状态信息。
     */
    class UploadSessionInfo {
        private String uploadId;
        private String fileName;
        private Long totalSize;
        private Long uploadedSize;
        private String uploadStatus; // INITIATED / IN_PROGRESS / COMPLETED / ABORTED
        private String bucketName;

        public UploadSessionInfo() {
        }

        public String getUploadId() {
            return uploadId;
        }

        public void setUploadId(String uploadId) {
            this.uploadId = uploadId;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public Long getTotalSize() {
            return totalSize;
        }

        public void setTotalSize(Long totalSize) {
            this.totalSize = totalSize;
        }

        public Long getUploadedSize() {
            return uploadedSize;
        }

        public void setUploadedSize(Long uploadedSize) {
            this.uploadedSize = uploadedSize;
        }

        public String getUploadStatus() {
            return uploadStatus;
        }

        public void setUploadStatus(String uploadStatus) {
            this.uploadStatus = uploadStatus;
        }

        public String getBucketName() {
            return bucketName;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }
    }
}
