package com.github.zeng.alt.captcha.producer;

import com.github.zeng.alt.captcha.model.CaptchaChallenge;
import com.github.zeng.alt.captcha.model.CaptchaType;

public interface CaptchaProducer {

    CaptchaType type();

    CaptchaChallenge produce();
}
