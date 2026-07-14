package com.github.zeng.alt.captcha;

import com.github.zeng.alt.captcha.config.CaptchaAutoConfiguration;
import com.github.zeng.alt.captcha.config.CaptchaProperties;
import com.github.zeng.alt.captcha.core.CaptchaTemplate;
import com.github.zeng.alt.captcha.core.CaptchaTemplateImpl;
import com.github.zeng.alt.captcha.model.CaptchaChallenge;
import com.github.zeng.alt.captcha.model.CaptchaInfo;
import com.github.zeng.alt.captcha.producer.ArithmeticCaptchaProducer;
import com.github.zeng.alt.captcha.producer.CaptchaProducer;
import com.github.zeng.alt.captcha.producer.ImageCaptchaProducer;
import com.github.zeng.alt.captcha.producer.RandomCodeProducer;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class CaptchaRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        registerType(hints,
                CaptchaAutoConfiguration.class,
                CaptchaProperties.class,
                CaptchaProperties.Image.class,
                CaptchaProperties.Code.class,

                CaptchaTemplate.class,
                CaptchaTemplateImpl.class,

                CaptchaProducer.class,
                RandomCodeProducer.class,
                ArithmeticCaptchaProducer.class,
                ImageCaptchaProducer.class,

                CaptchaInfo.class,
                CaptchaChallenge.class);

        registerType(hints,
                BufferedImage.class,
                Graphics2D.class,
                Color.class,
                Font.class,
                BasicStroke.class,
                RenderingHints.class,
                RenderingHints.Key.class,
                Image.class,
                AffineTransform.class);

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
