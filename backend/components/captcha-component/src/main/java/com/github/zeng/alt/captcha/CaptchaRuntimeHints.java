package com.github.zeng.alt.captcha;

import com.github.zeng.alt.captcha.config.CaptchaAutoConfiguration;
import com.github.zeng.alt.captcha.config.CaptchaProperties;
import com.github.zeng.alt.captcha.core.CaptchaRenderer;
import com.github.zeng.alt.captcha.core.CaptchaTemplate;
import com.github.zeng.alt.captcha.core.CaptchaTemplateImpl;
import com.github.zeng.alt.captcha.model.CaptchaChallenge;
import com.github.zeng.alt.captcha.model.CaptchaInfo;
import com.github.zeng.alt.captcha.producer.ArithmeticCaptchaProducer;
import com.github.zeng.alt.captcha.producer.CaptchaProducer;
import com.github.zeng.alt.captcha.producer.RandomCodeProducer;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

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
                CaptchaRenderer.class,

                CaptchaProducer.class,
                RandomCodeProducer.class,
                ArithmeticCaptchaProducer.class,

                CaptchaInfo.class,
                CaptchaChallenge.class);

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
