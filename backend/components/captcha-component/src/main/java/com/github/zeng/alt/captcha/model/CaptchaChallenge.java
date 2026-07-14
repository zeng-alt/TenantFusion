package com.github.zeng.alt.captcha.model;

public class CaptchaChallenge {

    private String code;
    private String imageBase64;
    private String expression;
    private long expireIn;

    public CaptchaChallenge() {
    }

    public CaptchaChallenge(String code, String imageBase64, String expression, long expireIn) {
        this.code = code;
        this.imageBase64 = imageBase64;
        this.expression = expression;
        this.expireIn = expireIn;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public long getExpireIn() {
        return expireIn;
    }

    public void setExpireIn(long expireIn) {
        this.expireIn = expireIn;
    }
}
