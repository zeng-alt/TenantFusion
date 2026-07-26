package com.github.zeng.alt.captcha.model;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
import java.util.Objects;
import java.util.function.Consumer;

public final class CaptchaInfo {

    private final String key;
    private final byte[] imageBytes;
    private final String expression;
    private final long expireIn;

    private CaptchaInfo(String key, byte[] imageBytes, String expression, long expireIn) {
        this.key = Objects.requireNonNull(key, "key must not be null");
        this.imageBytes = Objects.requireNonNull(imageBytes, "imageBytes must not be null");
        this.expression = expression;
        this.expireIn = expireIn;
    }

    public static CaptchaInfo of(String key, byte[] imageBytes, long expireIn) {
        return new CaptchaInfo(key, imageBytes, null, expireIn);
    }

    public static CaptchaInfo of(String key, byte[] imageBytes, String expression, long expireIn) {
        return new CaptchaInfo(key, imageBytes, expression, expireIn);
    }

    public String getKey() {
        return key;
    }

    public byte[] getBytes() {
        return imageBytes;
    }

    public String getExpression() {
        return expression;
    }

    public long getExpireIn() {
        return expireIn;
    }

    public String toBase64() {
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
    }

    public CaptchaInfo cookie(HttpServletResponse response, String name) {
        return cookie(response, name, c -> {});
    }

    public CaptchaInfo cookie(HttpServletResponse response, String name, Consumer<Cookie> customizer) {
        Cookie cookie = new Cookie(name, key);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) expireIn);
        customizer.accept(cookie);
        response.addCookie(cookie);
        return this;
    }

    public CaptchaInfo writeTo(OutputStream out) throws IOException {
        out.write(imageBytes);
        out.flush();
        return this;
    }

    public CaptchaInfo writeTo(HttpServletResponse response) throws IOException {
        response.setContentType("image/png");
        return writeTo(response.getOutputStream());
    }
}
