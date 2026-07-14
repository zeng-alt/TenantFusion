package com.github.zeng.alt.captcha.model;

public class CaptchaInfo {

    private String key;
    private String imageBase64;
    private String expression;
    private long expireIn;

    public CaptchaInfo() {
    }

    public CaptchaInfo(String key, String imageBase64, String expression, long expireIn) {
        this.key = key;
        this.imageBase64 = imageBase64;
        this.expression = expression;
        this.expireIn = expireIn;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
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
