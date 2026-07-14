package com.github.zeng.alt.captcha.producer;

import com.github.zeng.alt.captcha.model.CaptchaChallenge;

public interface CaptchaProducer {

    String type();

    CaptchaChallenge produce();
}
