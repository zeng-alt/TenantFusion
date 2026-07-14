package com.github.zeng.alt.captcha.core;

import com.github.zeng.alt.captcha.model.CaptchaInfo;

public interface CaptchaTemplate {

    CaptchaInfo generate();

    CaptchaInfo generate(String type);

    boolean verify(String key, String code);
}
