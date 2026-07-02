package com.github.zeng.alt.oss;

import java.io.InputStream;

/**
 * 缩略图生成服务。
 * <p>
 * 在图片文件上传时自动识别文件类型并生成缩略图，
 * 生成的缩略图会上传到对象存储中，与原始文件关联。
 * 缩略图大小可通过 {@link ThumbnailProperties} 配置。
 * <p>
 * 实现需要考虑 GraalVM native-image 兼容性，
 * 需在 {@code RuntimeHintsRegistrar} 中注册 ImageIO 相关类型。
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
public interface ThumbnailService {

    /**
     * 生成并上传缩略图。
     * <p>
     * 检测文件类型，若为图片则生成缩略图并上传到对象存储，
     * 返回缩略图的访问信息。若非图片类型则返回 {@code null}。
     *
     * @param originalData       原始文件数据
     * @param originalFileName   原始文件名
     * @param contentType        MIME 类型
     * @param ossTemplate        用于上传缩略图的 OSS 模板
     * @param bucketStrategy     桶策略（决定缩略图的存储位置）
     * @return 缩略图文件信息，非图片返回 {@code null}
     */
    OssFileInfo generateThumbnail(byte[] originalData, String originalFileName,
                                  String contentType, OssTemplate ossTemplate,
                                  BucketStrategy bucketStrategy);

    /**
     * 判断给定文件是否支持缩略图生成。
     *
     * @param fileName    文件名
     * @param contentType MIME 类型
     * @return true 支持
     */
    boolean supports(String fileName, String contentType);
}
