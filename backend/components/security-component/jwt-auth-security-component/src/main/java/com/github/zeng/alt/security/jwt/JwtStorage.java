package com.github.zeng.alt.security.jwt;

import com.github.zeng.alt.storage.StorageTemplate;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

/**
 * @author zengJiaJun
 * @since 2026年07月25日
 * @version 1.0
 */
@RequiredArgsConstructor
public class JwtStorage {

    private final JwtProperties jwtProperties;
    private final StorageTemplate storageTemplate;

    public <T> void setAccessToken(String key, T value) {
        storageTemplate.opsForString().set(key, value, Duration.ofSeconds(jwtProperties.getExpiration()));
    }

    public <T> void setRefreshToken(String key, T value) {
        storageTemplate.opsForString().set(key, value, Duration.ofSeconds(jwtProperties.getRememberMeExpiration()));
    }

    public boolean hasToken(String key) {
        return storageTemplate.opsForString().hasKey(key);
    }


    public void removeToken(String key) {
        storageTemplate.opsForString().delete(key);
    }


    public long removeAllToken(String key) {
        return storageTemplate.opsForString().deleteByPattern(key + ":*");
    }

}
