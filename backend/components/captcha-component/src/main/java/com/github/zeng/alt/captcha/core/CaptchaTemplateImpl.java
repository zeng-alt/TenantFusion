package com.github.zeng.alt.captcha.core;

import com.github.zeng.alt.captcha.config.CaptchaProperties;
import com.github.zeng.alt.captcha.model.CaptchaChallenge;
import com.github.zeng.alt.captcha.model.CaptchaInfo;
import com.github.zeng.alt.captcha.producer.CaptchaProducer;
import com.github.zeng.alt.storage.StorageTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class CaptchaTemplateImpl implements CaptchaTemplate {

    private static final String KEY_PREFIX = "captcha:";

    private final StorageTemplate storageTemplate;
    private final CaptchaProperties properties;
    private final Map<String, CaptchaProducer> producerMap;

    public CaptchaTemplateImpl(StorageTemplate storageTemplate,
                               CaptchaProperties properties,
                               List<CaptchaProducer> producers) {
        this.storageTemplate = storageTemplate;
        this.properties = properties;
        this.producerMap = producers.stream()
                .collect(Collectors.toConcurrentMap(
                        CaptchaProducer::type,
                        p -> p,
                        (a, b) -> b
                ));
    }

    @Override
    public CaptchaInfo generate() {
        return generate(properties.getDefaultType());
    }

    @Override
    public CaptchaInfo generate(String type) {
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

        return new CaptchaInfo(
                key,
                challenge.getImageBase64(),
                challenge.getExpression(),
                expireIn
        );
    }

    @Override
    public boolean verify(String key, String code) {
        if (key == null || code == null) {
            return false;
        }
        String cacheKey = KEY_PREFIX + key;
        String stored = storageTemplate.opsForString().get(cacheKey, String.class);
        storageTemplate.delete(cacheKey);
        return code.equals(stored);
    }

    private CaptchaProducer resolveProducer(String type) {
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
