package com.github.zeng.alt.captcha.config;

import com.github.zeng.alt.captcha.model.CaptchaType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "alt.captcha")
public class CaptchaProperties {

    private long expireIn = 300;

    private CaptchaType type = CaptchaType.CODE;

    private Image image = new Image();

    private Code code = new Code();

    @Setter
    @Getter
    public static class Image {

        private int width = 120;

        private int height = 60;

    }

    @Setter
    @Getter
    public static class Code {

        private int length = 4;

    }
}
