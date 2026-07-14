package com.github.zeng.alt.captcha.producer;

import com.github.zeng.alt.captcha.model.CaptchaChallenge;

import java.security.SecureRandom;

public class RandomCodeProducer implements CaptchaProducer {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final int length;

    public RandomCodeProducer(int length) {
        this.length = length;
    }

    @Override
    public String type() {
        return "code";
    }

    @Override
    public CaptchaChallenge produce() {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return new CaptchaChallenge(sb.toString(), null, null, 300);
    }

    public int getLength() {
        return length;
    }
}
