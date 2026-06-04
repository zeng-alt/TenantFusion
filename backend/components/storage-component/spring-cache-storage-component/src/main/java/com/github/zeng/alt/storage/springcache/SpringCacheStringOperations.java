package com.github.zeng.alt.storage.springcache;

import com.github.zeng.alt.storage.api.CacheStringOperations;
import com.github.zeng.alt.storage.api.KeyPrefixStrategy;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Spring Data Redis String 结构操作实现
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public class SpringCacheStringOperations implements CacheStringOperations {

    private final StringRedisTemplate redisTemplate;
    private final KeyPrefixStrategy keyPrefixStrategy;

    public SpringCacheStringOperations(StringRedisTemplate redisTemplate, KeyPrefixStrategy keyPrefixStrategy) {
        this.redisTemplate = redisTemplate;
        this.keyPrefixStrategy = keyPrefixStrategy;
    }

    private String wrap(String key) {
        return keyPrefixStrategy.wrapKey(key);
    }

    @Override
    public void set(String key, String value) {
        redisTemplate.opsForValue().set(wrap(key), value);
    }

    @Override
    public void set(String key, String value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(wrap(key), value, timeout, unit);
    }

    @Override
    public String get(String key) {
        return redisTemplate.opsForValue().get(wrap(key));
    }

    @Override
    public Boolean setIfAbsent(String key, String value) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(wrap(key), value));
    }

    @Override
    public Boolean setIfAbsent(String key, String value, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(wrap(key), value, timeout, unit));
    }

    @Override
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.expire(wrap(key), timeout, unit));
    }

    @Override
    public Long getExpire(String key) {
        return redisTemplate.getExpire(wrap(key));
    }

    @Override
    public Boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(wrap(key)));
    }

    @Override
    public Boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(wrap(key)));
    }

    @Override
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(wrap(key));
    }

    @Override
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(wrap(key), delta);
    }
}
