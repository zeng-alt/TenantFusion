package com.github.zeng.alt.captcha.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "alt.captcha")
public class CaptchaProperties {

    /**
     * 验证码过期时间（秒），默认 300（5 分钟）
     */
    private long expireIn = 300;

    /**
     * 默认验证码类型：image / arithmetic / code
     */
    private String defaultType = "image";

    /**
     * 图片验证码配置
     */
    private Image image = new Image();

    /**
     * 随机码验证码配置
     */
    private Code code = new Code();

    public long getExpireIn() {
        return expireIn;
    }

    public void setExpireIn(long expireIn) {
        this.expireIn = expireIn;
    }

    public String getDefaultType() {
        return defaultType;
    }

    public void setDefaultType(String defaultType) {
        this.defaultType = defaultType;
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public Code getCode() {
        return code;
    }

    public void setCode(Code code) {
        this.code = code;
    }

    public static class Image {

        /**
         * 图片宽度
         */
        private int width = 80;

        /**
         * 图片高度
         */
        private int height = 40;

        /**
         * 字符个数
         */
        private int length = 4;

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

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }
    }

    public static class Code {

        /**
         * 随机码长度
         */
        private int length = 4;

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }
    }
}
