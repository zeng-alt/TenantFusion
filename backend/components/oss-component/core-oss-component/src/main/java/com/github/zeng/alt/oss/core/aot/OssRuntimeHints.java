package com.github.zeng.alt.oss.core.aot;

import com.github.zeng.alt.oss.*;
import com.github.zeng.alt.oss.core.*;
import com.github.zeng.alt.oss.core.upload.UploadController;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageInputStreamSpi;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageWriterSpi;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Iterator;

/**
 * OSS 模块 GraalVM native image RuntimeHints。
 * <p>
 * 注册本模块中需要在运行时通过反射访问的类，
 * 以及缩略图生成所需的 ImageIO / AWT SPI 类型。
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 2.0
 *
 * @see OssCoreAutoConfiguration
 */
public class OssRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {

        // ========== API ==========
        registerType(hints,
                OssTemplate.class,
                OssFileInfo.class,
                OssFileRecordService.class,
                OssProperties.class,
                OssConnectionManager.class,
                BucketStrategy.class,
                FileType.class,
                MultipartUploadService.class,
                UploadPartInfo.class,
                ThumbnailService.class,
                ThumbnailProperties.class,
                MultipartUploadService.UploadSessionInfo.class);

        // ========== Core ==========
        registerType(hints,
                RefreshableOssTemplate.class,
                S3OssTemplate.class,
                DefaultOssConnectionManager.class,
                OssCoreAutoConfiguration.class,
                OssException.class,
                DefaultBucketStrategy.class,
                S3MultipartUploadService.class,
                DefaultThumbnailService.class,
                UploadController.class,
                UploadController.InitiateUploadRequest.class,
                UploadController.CompleteUploadRequest.class);

        // ========== ImageIO / AWT（缩略图生成） ==========
        registerImageIoTypes(hints, classLoader);

        // ========== AWT rendering hints ==========
        registerType(hints, RenderingHints.class);
        registerType(hints, RenderingHints.Key.class);
        registerType(hints, Color.class);
        registerType(hints, BufferedImage.class);
        registerType(hints, Graphics2D.class);
        registerType(hints, Image.class);
        registerType(hints, java.awt.geom.AffineTransform.class);
    }

    /**
     * 注册 ImageIO SPI 类型（Reader、Writer、Stream 等）。
     * <p>
     * GraalVM native-image 下 ImageIO 需要显式注册 SPI 实现才能正常工作。
     * 通过 {@link IIORegistry} 获取已注册的 SPI 并逐类注册。
     */
    private void registerImageIoTypes(RuntimeHints hints, ClassLoader classLoader) {
        try {
            IIORegistry registry = IIORegistry.getDefaultInstance();

            // 注册 ImageReaderSpi
            Iterator<ImageReaderSpi> readerSpis = registry.getServiceProviders(ImageReaderSpi.class, true);
            while (readerSpis.hasNext()) {
                ImageReaderSpi spi = readerSpis.next();
                registerType(hints, spi.getClass());
                // 注册 reader 返回的 ImageReader 类型
                try {
                    ImageReaderSpi provider = readerSpis.next();
                } catch (Exception ignored) {
                }
            }

            // 注册 ImageWriterSpi
            Iterator<ImageWriterSpi> writerSpis = registry.getServiceProviders(ImageWriterSpi.class, true);
            while (writerSpis.hasNext()) {
                ImageWriterSpi spi = writerSpis.next();
                registerType(hints, spi.getClass());
            }

            // 注册 ImageInputStreamSpi
            Iterator<ImageInputStreamSpi> streamSpis = registry.getServiceProviders(ImageInputStreamSpi.class, true);
            while (streamSpis.hasNext()) {
                ImageInputStreamSpi spi = streamSpis.next();
                registerType(hints, spi.getClass());
            }
        } catch (Exception e) {
            // ImageIO 不可用时静默忽略（如无头环境）
        }

        // 显式注册 ImageIO 类本身（反射调用 write/read）
        hints.reflection().registerType(ImageIO.class,
                MemberCategory.INVOKE_DECLARED_METHODS);
    }

    private static void registerType(RuntimeHints hints, Class<?>... classes) {
        for (Class<?> clazz : classes) {
            hints.reflection().registerType(clazz,
                    MemberCategory.INTROSPECT_DECLARED_METHODS,
                    MemberCategory.DECLARED_FIELDS,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_METHODS);
        }
    }
}
