package com.github.zeng.alt.oss;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 文件类型分类。
 * <p>
 * 用于根据文件扩展名或 MIME 类型对上传文件进行分类，
 * 支持自动桶策略（不同类型分配到不同存储桶）和缩略图生成（仅图片类型）。
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
public enum FileType {

    /** 图片：jpg, jpeg, png, gif, bmp, webp, svg, ico */
    IMAGE,
    /** 文档：pdf, doc, docx, xls, xlsx, ppt, pptx, txt, csv, md */
    DOCUMENT,
    /** 压缩包：zip, rar, 7z, tar, gz, bz2, xz, zst */
    ARCHIVE,
    /** 音频：mp3, wav, ogg, flac, aac, wma, m4a */
    AUDIO,
    /** 视频：mp4, avi, mkv, wmv, mov, flv, webm */
    VIDEO,
    /** 其他类型 */
    OTHER;

    /** 图片扩展名集合 */
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico", "tiff", "tif", "heic", "heif"
    );

    /** 文档扩展名集合 */
    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv",
            "md", "markdown", "rtf", "odt", "ods", "odp", "wps"
    );

    /** 压缩包扩展名集合 */
    private static final Set<String> ARCHIVE_EXTENSIONS = Set.of(
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "zst", "tgz", "lz4", "snappy"
    );

    /** 音频扩展名集合 */
    private static final Set<String> AUDIO_EXTENSIONS = Set.of(
            "mp3", "wav", "ogg", "flac", "aac", "wma", "m4a", "opus", "ape", "midi", "mid"
    );

    /** 视频扩展名集合 */
    private static final Set<String> VIDEO_EXTENSIONS = Set.of(
            "mp4", "avi", "mkv", "wmv", "mov", "flv", "webm", "mpeg", "mpg", "3gp", "ts", "mts"
    );

    /** 扩展名 → FileType 映射（全小写） */
    private static final Map<String, FileType> EXTENSION_MAP = buildExtensionMap();

    private static Map<String, FileType> buildExtensionMap() {
        var map = new java.util.HashMap<String, FileType>();
        IMAGE_EXTENSIONS.forEach(ext -> map.put(ext, IMAGE));
        DOCUMENT_EXTENSIONS.forEach(ext -> map.put(ext, DOCUMENT));
        ARCHIVE_EXTENSIONS.forEach(ext -> map.put(ext, ARCHIVE));
        AUDIO_EXTENSIONS.forEach(ext -> map.put(ext, AUDIO));
        VIDEO_EXTENSIONS.forEach(ext -> map.put(ext, VIDEO));
        return Map.copyOf(map);
    }

    // ==================== 静态工具方法 ====================

    /**
     * 根据原始文件名或文件路径推断文件类型。
     *
     * @param fileName 文件名（含扩展名）
     * @return 文件类型，无法识别返回 {@link #OTHER}
     */
    public static FileType fromFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return OTHER;
        }
        String lower = fileName.toLowerCase();
        int dotIndex = lower.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex >= lower.length() - 1) {
            return OTHER;
        }
        String ext = lower.substring(dotIndex + 1);
        return EXTENSION_MAP.getOrDefault(ext, OTHER);
    }

    /**
     * 根据 MIME 类型推断文件类型。
     *
     * @param contentType MIME 类型，如 {@code image/jpeg}、{@code application/pdf}
     * @return 文件类型，无法识别返回 {@link #OTHER}
     */
    public static FileType fromContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return OTHER;
        }
        String lower = contentType.toLowerCase();
        if (lower.startsWith("image/")) return IMAGE;
        if (lower.startsWith("video/")) return VIDEO;
        if (lower.startsWith("audio/")) return AUDIO;
        if (lower.contains("pdf") || lower.contains("document") || lower.contains("spreadsheet")
                || lower.contains("presentation") || lower.contains("text/")) {
            return DOCUMENT;
        }
        if (lower.contains("zip") || lower.contains("rar") || lower.contains("tar")
                || lower.contains("gzip") || lower.contains("compress")) {
            return ARCHIVE;
        }
        return OTHER;
    }

    /**
     * 综合文件名和 MIME 类型推断文件类型，文件名优先。
     *
     * @param fileName    原始文件名
     * @param contentType MIME 类型
     * @return 文件类型
     */
    public static FileType detect(String fileName, String contentType) {
        FileType fromName = fromFileName(fileName);
        if (fromName != OTHER) {
            return fromName;
        }
        return fromContentType(contentType);
    }

    // ==================== 实例方法 ====================

    /**
     * 当前类型是否为图片。
     */
    public boolean isImage() {
        return this == IMAGE;
    }

    /**
     * 当前类型是否支持缩略图生成。
     */
    public boolean isThumbnailSupported() {
        return this == IMAGE;
    }

    /**
     * 获取当前类型对应的默认桶名后缀。
     * <p>
     * 例如 {@code IMAGE} → {@code images}，{@code DOCUMENT} → {@code documents}。
     */
    public String getBucketSuffix() {
        return switch (this) {
            case IMAGE -> "images";
            case DOCUMENT -> "documents";
            case ARCHIVE -> "archives";
            case AUDIO -> "audio";
            case VIDEO -> "video";
            case OTHER -> "others";
        };
    }
}
