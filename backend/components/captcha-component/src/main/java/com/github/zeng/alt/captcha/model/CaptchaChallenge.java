package com.github.zeng.alt.captcha.model;

import java.util.Objects;

public final class CaptchaChallenge {

    private final String code;
    private final byte[] imageBytes;
    private final String expression;
    private final long expireIn;

    public CaptchaChallenge(String code, byte[] imageBytes, String expression, long expireIn) {
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.imageBytes = Objects.requireNonNull(imageBytes, "imageBytes must not be null");
        this.expression = expression;
        this.expireIn = expireIn;
    }

    public String getCode() {
        return code;
    }

    public byte[] getImageBytes() {
        return imageBytes;
    }

    public String getExpression() {
        return expression;
    }

    public long getExpireIn() {
        return expireIn;
    }
}
