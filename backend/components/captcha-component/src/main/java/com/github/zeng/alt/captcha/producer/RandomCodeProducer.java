package com.github.zeng.alt.captcha.producer;

import com.github.zeng.alt.captcha.core.CaptchaRenderer;
import com.github.zeng.alt.captcha.model.CaptchaChallenge;
import com.github.zeng.alt.captcha.model.CaptchaType;

import java.security.SecureRandom;

public class RandomCodeProducer implements CaptchaProducer {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CaptchaRenderer renderer;
    private final int length;

    public RandomCodeProducer(CaptchaRenderer renderer, int length) {
        this.renderer = renderer;
        this.length = length;
    }

    @Override
    public CaptchaType type() {
        return CaptchaType.CODE;
    }

    @Override
    public CaptchaChallenge produce() {
        String code = generateCode();
        byte[] imageBytes = renderer.render(code);
        return new CaptchaChallenge(code, imageBytes, null, 300);
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    public int getLength() {
        return length;
    }
}
