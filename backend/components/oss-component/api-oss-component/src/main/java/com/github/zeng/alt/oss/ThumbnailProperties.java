package com.github.zeng.alt.oss;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 缩略图生成配置属性。
 * <p>
 * 前缀：{@code oss.thumbnail}
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
@ConfigurationProperties(prefix = "oss.thumbnail")
public class ThumbnailProperties {

    /** 是否启用缩略图生成，默认启用 */
    private boolean enabled = true;

    /** 缩略图宽度（像素），默认 200 */
    private int width = 200;

    /** 缩略图高度（像素），默认 200 */
    private int height = 200;

    /** 是否保持原始宽高比，默认 true */
    private boolean keepAspectRatio = true;

    /** 缩略图质量（0.0 ~ 1.0），默认 0.8 */
    private double quality = 0.8;

    /** 缩略图输出格式，默认 {@code jpeg} */
    private String format = "jpeg";

    /** 缩略图文件后缀，默认 {@code _thumb}（在文件名后追加，如 photo_thumb.jpg） */
    private String suffix = "_thumb";

    /** 缩略图存储路径前缀（可选，在桶中单独目录存放） */
    private String pathPrefix = "thumbnails";

    /** 最大原始图片宽度（像素），超过此尺寸才生成缩略图，默认 0（始终生成） */
    private int maxOriginalWidth = 0;

    /** 最大原始图片高度（像素），超过此尺寸才生成缩略图，默认 0（始终生成） */
    private int maxOriginalHeight = 0;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isKeepAspectRatio() {
        return keepAspectRatio;
    }

    public void setKeepAspectRatio(boolean keepAspectRatio) {
        this.keepAspectRatio = keepAspectRatio;
    }

    public double getQuality() {
        return quality;
    }

    public void setQuality(double quality) {
        this.quality = quality;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getPathPrefix() {
        return pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    public int getMaxOriginalWidth() {
        return maxOriginalWidth;
    }

    public void setMaxOriginalWidth(int maxOriginalWidth) {
        this.maxOriginalWidth = maxOriginalWidth;
    }

    public int getMaxOriginalHeight() {
        return maxOriginalHeight;
    }

    public void setMaxOriginalHeight(int maxOriginalHeight) {
        this.maxOriginalHeight = maxOriginalHeight;
    }
}
