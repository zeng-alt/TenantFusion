package com.github.zeng.alt.oss;

/**
 * 存储桶策略。
 * <p>
 * 决定上传文件时使用的存储桶名称和路径前缀。
 * 不同类型的文件（图片、文档、音视频）可以分配到不同的存储桶，
 * 并按日期或其他规则组织路径，避免单桶/单目录下文件过多导致性能下降。
 * <p>
 * 默认实现会根据文件类型自动分配桶，并按 {@code /年/月/} 划分路径。
 * 可通过 {@link OssProperties} 自定义后缀或定制完整命名规则。
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
public interface BucketStrategy {

    /**
     * 根据文件信息确定存储桶名称。
     *
     * @param originalFileName 原始文件名
     * @param contentType      MIME 类型，可能为 {@code null}
     * @param fileType         文件类型分类
     * @return 存储桶名称
     */
    String determineBucketName(String originalFileName, String contentType, FileType fileType);

    /**
     * 根据文件信息确定文件在存储桶中的存储路径前缀（不含文件名）。
     * <p>
     * 返回的路径前缀以 {@code /} 结尾（如 {@code 2026/07/}），
     * 调用方拼接文件名后作为完整 key 上传。
     *
     * @param originalFileName 原始文件名
     * @param fileType         文件类型分类
     * @return 路径前缀，空字符串表示根目录
     */
    String determinePathPrefix(String originalFileName, FileType fileType);

    /**
     * 构建完整的存储文件名（路径 + 文件名）。
     *
     * @param originalFileName 原始文件名
     * @param contentType      MIME 类型
     * @param fileType         文件类型分类
     * @return 完整存储文件名（作为 S3 key 使用）
     */
    default String buildFileName(String originalFileName, String contentType, FileType fileType) {
        String prefix = determinePathPrefix(originalFileName, fileType);
        String bucket = determineBucketName(originalFileName, contentType, fileType);
        // bucket 由 OssTemplate 管理，此处只返回 key
        return prefix + originalFileName;
    }
}
