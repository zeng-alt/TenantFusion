package com.github.zeng.alt.oss.core.upload;

import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.oss.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 分片上传 REST 控制器（断点续传）。
 * <p>
 * 提供大文件分片上传的 REST API，支持断点续传。
 * 客户端流程：
 * <ol>
 *   <li>调用 {@code POST /api/oss/upload/init} 初始化上传，获取 uploadId</li>
 *   <li>将文件拆分为多个分片，每个分片调用 {@code POST /api/oss/upload/{uploadId}/parts/{partNumber}} 上传</li>
 *   <li>上传中断后，调用 {@code GET /api/oss/upload/{uploadId}/parts} 查询已上传的分片</li>
 *   <li>重新上传缺失分片后，调用 {@code POST /api/oss/upload/{uploadId}/complete} 完成合并</li>
 *   <li>或调用 {@code DELETE /api/oss/upload/{uploadId}} 取消上传</li>
 * </ol>
 * <p>
 * 通过 {@code oss.s3.upload.enabled=true} 启用（默认启用）。
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
@RestController
@RequestMapping("/api/oss/upload")
@Tag(name = "OSS 分片上传（断点续传）")
@ConditionalOnClass(name = "org.springframework.web.bind.annotation.RestController")
@ConditionalOnProperty(prefix = "oss.s3.upload", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(MultipartUploadService.class)
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    private final MultipartUploadService multipartUploadService;
    private final BucketStrategy bucketStrategy;
    private final OssTemplate ossTemplate;
    private final OssProperties ossProperties;

    public UploadController(MultipartUploadService multipartUploadService,
                            BucketStrategy bucketStrategy,
                            OssTemplate ossTemplate,
                            OssProperties ossProperties) {
        this.multipartUploadService = multipartUploadService;
        this.bucketStrategy = bucketStrategy;
        this.ossTemplate = ossTemplate;
        this.ossProperties = ossProperties;
    }

    // ==================== 初始化 ====================

    /**
     * 初始化分片上传。
     *
     * @param request 初始化请求
     * @return uploadId
     */
    @PostMapping("/init")
    @Operation(summary = "初始化分片上传")
    public RestResponse<Map<String, Object>> initiateUpload(@RequestBody InitiateUploadRequest request) {
        String uploadId = multipartUploadService.initiateUpload(
                request.getFileName(),
                request.getContentType(),
                request.getTotalSize()
        );
        log.info("Upload initiated: fileName={}, uploadId={}", request.getFileName(), uploadId);
        return RestResponse.success(Map.of(
                "uploadId", uploadId,
                "fileName", request.getFileName(),
                "totalSize", request.getTotalSize(),
                "partSize", 5_242_880 // 5MB default, 可从前端传递
        ));
    }

    // ==================== 上传分片 ====================

    /**
     * 上传单个分片。
     *
     * @param uploadId   上传会话 ID
     * @param partNumber 分片编号（从 1 开始）
     * @param file       分片文件
     * @return 分片上传结果
     */
    @PostMapping("/{uploadId}/parts/{partNumber}")
    @Operation(summary = "上传分片")
    public RestResponse<UploadPartInfo> uploadPart(
            @PathVariable String uploadId,
            @PathVariable int partNumber,
            @RequestParam("file") MultipartFile file) throws IOException {

        long partSize = file.getSize();
        try (InputStream data = file.getInputStream()) {
            UploadPartInfo result = multipartUploadService.uploadPart(uploadId, partNumber, partSize, data);
            log.debug("Part uploaded: uploadId={}, partNumber={}, size={}", uploadId, partNumber, partSize);
            return RestResponse.success(result);
        }
    }

    /**
     * 上传分片（二进制流方式，用于非 multipart 场景）。
     *
     * @param uploadId   上传会话 ID
     * @param partNumber 分片编号
     * @param body       分片数据字节流
     */
    @PostMapping(value = "/{uploadId}/parts/{partNumber}/raw", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "上传分片（二进制流）")
    public RestResponse<UploadPartInfo> uploadPartRaw(
            @PathVariable String uploadId,
            @PathVariable int partNumber,
            InputStream body) throws IOException {

        // 读取原始流数据
        byte[] data = body.readAllBytes();
        long partSize = data.length;
        try (InputStream dataStream = new java.io.ByteArrayInputStream(data)) {
            UploadPartInfo result = multipartUploadService.uploadPart(uploadId, partNumber, partSize, dataStream);
            return RestResponse.success(result);
        }
    }

    // ==================== 完成 / 取消 ====================

    /**
     * 完成分片上传。
     *
     * @param uploadId     上传会话 ID
     * @param completeRequest 完成请求（可包含最终文件名等）
     * @return 文件信息
     */
    @PostMapping("/{uploadId}/complete")
    @Operation(summary = "完成分片上传")
    public RestResponse<OssFileInfo> completeUpload(
            @PathVariable String uploadId,
            @RequestBody(required = false) CompleteUploadRequest completeRequest) {

        String fileName = completeRequest != null ? completeRequest.getFileName() : uploadId;

        // 确定目标桶：优先使用桶策略，否则使用默认配置桶
        String bucketName;
        if (bucketStrategy != null) {
            FileType fileType = FileType.fromFileName(fileName);
            bucketName = bucketStrategy.determineBucketName(fileName, null, fileType);
        } else {
            bucketName = ossProperties.getBucketName();
        }

        OssFileInfo result = multipartUploadService.completeUpload(uploadId, bucketName, fileName, null);
        log.info("Upload completed: fileName={}, uploadId={}", fileName, uploadId);
        return RestResponse.success(result);
    }

    /**
     * 取消分片上传。
     */
    @DeleteMapping("/{uploadId}")
    @Operation(summary = "取消分片上传")
    public RestResponse<Void> abortUpload(@PathVariable String uploadId) {
        multipartUploadService.abortUpload(uploadId);
        log.info("Upload aborted: uploadId={}", uploadId);
        return RestResponse.success();
    }

    // ==================== 查询 ====================

    /**
     * 查询已上传的分片列表（用于断点续传）。
     */
    @GetMapping("/{uploadId}/parts")
    @Operation(summary = "查询已上传分片列表（断点续传）")
    public RestResponse<List<UploadPartInfo>> listParts(@PathVariable String uploadId) {
        List<UploadPartInfo> parts = multipartUploadService.listParts(uploadId);
        return RestResponse.success(parts);
    }

    /**
     * 查询上传状态。
     */
    @GetMapping("/{uploadId}")
    @Operation(summary = "查询上传状态")
    public RestResponse<MultipartUploadService.UploadSessionInfo> getUploadStatus(
            @PathVariable String uploadId) {
        MultipartUploadService.UploadSessionInfo status = multipartUploadService.getUploadStatus(uploadId);
        if (status == null) {
            return RestResponse.success(new MultipartUploadService.UploadSessionInfo());
        }
        return RestResponse.success(status);
    }

    // ==================== 请求/响应 DTO ====================

    /**
     * 初始化上传请求体。
     */
    public static class InitiateUploadRequest {
        private String fileName;
        private String contentType;
        private Long totalSize;

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public Long getTotalSize() {
            return totalSize;
        }

        public void setTotalSize(Long totalSize) {
            this.totalSize = totalSize;
        }
    }

    /**
     * 完成上传请求体。
     */
    public static class CompleteUploadRequest {
        private String fileName;

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
    }
}
