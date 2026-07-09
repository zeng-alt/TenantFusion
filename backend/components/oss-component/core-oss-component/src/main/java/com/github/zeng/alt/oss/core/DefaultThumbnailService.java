package com.github.zeng.alt.oss.core;

import com.github.zeng.alt.oss.*;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.core.log.LogMessage;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 基于 Java 2D / ImageIO 的默认缩略图生成服务。
 * <p>
 * 使用 JDK 内置的 ImageIO 读取图片，通过 Graphics2D 绘制缩略图，
 * 最后将缩略图上传到与原始文件相同的对象存储桶中。
 * <p>
 * 支持格式：JPEG、PNG、GIF、BMP、WEBP（需 JDK 版本支持）。
 * <p>
 * 注意：GraalVM native-image 下需注册 ImageIO SPI 服务类型，
 * 参考 {@link com.github.zeng.alt.oss.core.aot.OssRuntimeHints}。
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
@CommonsLog
public class DefaultThumbnailService implements ThumbnailService {

    private final ThumbnailProperties thumbnailProperties;

    public DefaultThumbnailService(ThumbnailProperties thumbnailProperties) {
        this.thumbnailProperties = thumbnailProperties;
    }

    @Override
    public boolean supports(String fileName, String contentType) {
        if (!thumbnailProperties.isEnabled()) {
            return false;
        }
        FileType fileType = FileType.detect(fileName, contentType);
        return fileType.isThumbnailSupported();
    }

    @Override
    public OssFileInfo generateThumbnail(byte[] originalData, String originalFileName,
                                         String contentType, OssTemplate ossTemplate,
                                         BucketStrategy bucketStrategy) {
        if (!thumbnailProperties.isEnabled()) {
            log.debug("Thumbnail generation is disabled");
            return null;
        }

        FileType fileType = FileType.detect(originalFileName, contentType);
        if (!fileType.isThumbnailSupported()) {
            log.debug(LogMessage.format("Thumbnail not supported for file: %s, type: %s", originalFileName, fileType));
            return null;
        }

        try {
            // 1. 读取原始图片
            BufferedImage originalImage;
            try (InputStream is = new ByteArrayInputStream(originalData);
                 ImageInputStream iis = ImageIO.createImageInputStream(is)) {
                originalImage = ImageIO.read(iis);
            }

            if (originalImage == null) {
                log.warn(LogMessage.format("Cannot read image for thumbnail: %s", originalFileName));
                return null;
            }

            // 2. 检查是否需要生成缩略图（根据最大原始尺寸限制）
            if (shouldSkipThumbnail(originalImage)) {
                log.debug(LogMessage.format("Original image size within threshold, skipping thumbnail: %s (%sx%s)",
                        originalFileName, originalImage.getWidth(), originalImage.getHeight()));
                return null;
            }

            // 3. 计算缩略图尺寸
            int thumbWidth = thumbnailProperties.getWidth();
            int thumbHeight = thumbnailProperties.getHeight();
            if (thumbnailProperties.isKeepAspectRatio()) {
                Dimension scaled = getScaledDimension(
                        originalImage.getWidth(), originalImage.getHeight(),
                        thumbWidth, thumbHeight);
                thumbWidth = scaled.width;
                thumbHeight = scaled.height;
            }

            // 4. 生成缩略图
            BufferedImage thumbnailImage = new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = thumbnailImage.createGraphics();
            try {
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.drawImage(originalImage, 0, 0, thumbWidth, thumbHeight, null);
            } finally {
                g2d.dispose();
            }

            // 5. 编码缩略图为字节数组
            String format = thumbnailProperties.getFormat();
            byte[] thumbData;
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(thumbnailImage, format, baos);
                baos.flush();
                thumbData = baos.toByteArray();
            }

            // 6. 构建缩略图文件名
            String thumbFileName = buildThumbnailFileName(originalFileName, bucketStrategy);

            // 7. 上传缩略图到 OSS
            String thumbContentType = "image/" + format;
            OssFileInfo thumbInfo = ossTemplate.upload(thumbData, thumbFileName, thumbContentType);

            log.info(LogMessage.format("Thumbnail generated and uploaded: original=%s, thumbnail=%s, size=%sx%s",
                    originalFileName, thumbFileName, thumbWidth, thumbHeight));
            return thumbInfo;
        } catch (IOException e) {
            log.warn(LogMessage.format("Failed to generate thumbnail for %s: %s", originalFileName, e.getMessage()));
            return null;
        } catch (Exception e) {
            log.error("Unexpected error generating thumbnail for " + originalFileName, e);
            return null;
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 检查是否应跳过缩略图生成（原始图片尺寸小于阈值时跳过）。
     */
    private boolean shouldSkipThumbnail(BufferedImage image) {
        int maxW = thumbnailProperties.getMaxOriginalWidth();
        int maxH = thumbnailProperties.getMaxOriginalHeight();
        if (maxW > 0 && image.getWidth() < maxW) {
            return true;
        }
        return maxH > 0 && image.getHeight() < maxH;
    }

    /**
     * 在保持宽高比的前提下计算目标缩放尺寸。
     */
    private Dimension getScaledDimension(int originalWidth, int originalHeight,
                                          int targetWidth, int targetHeight) {
        double ratio = Math.min(
                (double) targetWidth / originalWidth,
                (double) targetHeight / originalHeight);
        int scaledW = (int) (originalWidth * ratio);
        int scaledH = (int) (originalHeight * ratio);
        return new Dimension(Math.max(scaledW, 1), Math.max(scaledH, 1));
    }

    /**
     * 构建缩略图文件名。
     * <p>
     * 规则：基于桶策略确定的路径，在原始文件名后追加缩略图后缀。
     * 如原始文件 {@code images/2026/07/photo.jpg} → {@code images/2026/07/photo_thumb.jpg}
     */
    private String buildThumbnailFileName(String originalFileName, BucketStrategy bucketStrategy) {
        // 先通过桶策略获得路径前缀（如 "2026/07/"）
        FileType fileType = FileType.fromFileName(originalFileName);
        String pathPrefix = "";
        if (bucketStrategy != null) {
            pathPrefix = bucketStrategy.determinePathPrefix(originalFileName, fileType);
        }

        String suffix = thumbnailProperties.getSuffix(); // 如 "_thumb"
        String format = thumbnailProperties.getFormat();  // 如 "jpeg"

        // 从原始文件名中提取名称（不含扩展名）
        String nameWithoutExt = originalFileName;
        String ext = format;
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            nameWithoutExt = originalFileName.substring(0, dotIndex);
            // 如果原始格式和缩略图格式相同，保留原始扩展名
            String originalExt = originalFileName.substring(dotIndex + 1);
            if (originalExt.equalsIgnoreCase("jpg") || originalExt.equalsIgnoreCase("jpeg")
                    || originalExt.equalsIgnoreCase("png") || originalExt.equalsIgnoreCase("gif")) {
                ext = originalExt;
            }
        }

        String thumbPathPrefix = thumbnailProperties.getPathPrefix(); // "thumbnails"
        return thumbPathPrefix + "/" + pathPrefix + nameWithoutExt + suffix + "." + ext;
    }
}
