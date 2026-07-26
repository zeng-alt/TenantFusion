package com.github.zeng.alt.captcha.core;

import com.github.zeng.alt.captcha.model.CaptchaInfo;
import com.github.zeng.alt.captcha.model.CaptchaType;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.function.Consumer;

public interface CaptchaTemplate {

    CaptchaInfo generate();

    CaptchaInfo generate(CaptchaType type);

    CaptchaInfo write(CaptchaType type, HttpServletResponse response) throws IOException;

    boolean verify(String key, String code);

    CaptchaTemplate deleteCookie(HttpServletResponse response, String name);

    CaptchaTemplate deleteCookie(HttpServletResponse response, String name, Consumer<Cookie> customizer);
}
