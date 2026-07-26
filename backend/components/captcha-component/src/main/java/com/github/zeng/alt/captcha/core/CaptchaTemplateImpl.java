package com.github.zeng.alt.captcha.core;

import com.github.zeng.alt.captcha.config.CaptchaProperties;
import com.github.zeng.alt.captcha.model.CaptchaChallenge;
import com.github.zeng.alt.captcha.model.CaptchaInfo;
import com.github.zeng.alt.captcha.model.CaptchaType;
import com.github.zeng.alt.captcha.producer.CaptchaProducer;
import com.github.zeng.alt.storage.StorageTemplate;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class CaptchaTemplateImpl implements CaptchaTemplate {

    private static final String KEY_PREFIX = "captcha:";

    private final StorageTemplate storageTemplate;
    private final CaptchaProperties properties;
    private final EnumMap<CaptchaType, CaptchaProducer> producerMap;

    public CaptchaTemplateImpl(StorageTemplate storageTemplate,
                               CaptchaProperties properties,
                               List<CaptchaProducer> producers) {
        this.storageTemplate = storageTemplate;
        this.properties = properties;
        this.producerMap = new EnumMap<>(CaptchaType.class);
        for (CaptchaProducer producer : producers) {
            this.producerMap.put(
                    producer.type(),
                    producer
            );
        }
    }

    @Override
    public CaptchaInfo generate() {
        return generate(properties.getType());
    }

    @Override
    public CaptchaInfo generate(CaptchaType type) {
        CaptchaProducer producer = resolveProducer(type);
        CaptchaChallenge challenge = producer.produce();
        String key = UUID.randomUUID().toString().replace("-", "");
        long expireIn = challenge.getExpireIn() > 0
                ? challenge.getExpireIn()
                : properties.getExpireIn();

        storageTemplate.opsForString().set(
                KEY_PREFIX + key,
                challenge.getCode(),
                Duration.ofSeconds(expireIn)
        );

        String expr = challenge.getExpression();
        return expr != null
                ? CaptchaInfo.of(key, challenge.getImageBytes(), expr, expireIn)
                : CaptchaInfo.of(key, challenge.getImageBytes(), expireIn);
    }

    @Override
    public CaptchaInfo write(CaptchaType type, HttpServletResponse response) throws IOException {
        return generate(type).writeTo(response);
    }

    @Override
    public boolean verify(String key, String code) {
        if (key == null || code == null) {
            return false;
        }
        String cacheKey = KEY_PREFIX + key;
        String stored = storageTemplate.opsForString().get(cacheKey, String.class);
        storageTemplate.delete(cacheKey);
        return code.equalsIgnoreCase(stored);
    }

    @Override
    public CaptchaTemplate deleteCookie(HttpServletResponse response, String name) {
        return deleteCookie(response, name, c -> {});
    }

    @Override
    public CaptchaTemplate deleteCookie(HttpServletResponse response, String name, Consumer<Cookie> customizer) {
        Cookie cookie = new Cookie(name, null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        customizer.accept(cookie);
        response.addCookie(cookie);
        return this;
    }

    private CaptchaProducer resolveProducer(CaptchaType type) {
        CaptchaProducer producer = producerMap.get(type);
        if (producer == null) {
            throw new IllegalArgumentException(
                    "Unsupported captcha type: " + type
                            + ". Available types: " + producerMap.keySet()
            );
        }
        return producer;
    }
}
