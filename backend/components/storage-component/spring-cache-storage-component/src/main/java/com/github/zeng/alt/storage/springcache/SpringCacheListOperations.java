package com.github.zeng.alt.storage.springcache;

import com.github.zeng.alt.storage.api.CacheListOperations;
import com.github.zeng.alt.storage.api.KeyPrefixStrategy;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Spring Data Redis List 结构操作实现
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public class SpringCacheListOperations implements CacheListOperations {

    private final StringRedisTemplate redisTemplate;
    private final KeyPrefixStrategy keyPrefixStrategy;

    public SpringCacheListOperations(StringRedisTemplate redisTemplate, KeyPrefixStrategy keyPrefixStrategy) {
        this.redisTemplate = redisTemplate;
        this.keyPrefixStrategy = keyPrefixStrategy;
    }

    private String wrap(String key) {
        return keyPrefixStrategy.wrapKey(key);
    }

    private ListOperations<String, String> ops() {
        return redisTemplate.opsForList();
    }

    @Override
    public Long leftPush(String key, String value) {
        return ops().leftPush(wrap(key), value);
    }

    @Override
    public Long leftPushAll(String key, String... values) {
        return ops().leftPushAll(wrap(key), values);
    }

    @Override
    public Long rightPush(String key, String value) {
        return ops().rightPush(wrap(key), value);
    }

    @Override
    public Long rightPushAll(String key, String... values) {
        return ops().rightPushAll(wrap(key), values);
    }

    @Override
    public String leftPop(String key) {
        return ops().leftPop(wrap(key));
    }

    @Override
    public String leftPop(String key, long timeout, TimeUnit unit) {
        return ops().leftPop(wrap(key), timeout, unit);
    }

    @Override
    public String rightPop(String key) {
        return ops().rightPop(wrap(key));
    }

    @Override
    public String rightPop(String key, long timeout, TimeUnit unit) {
        return ops().rightPop(wrap(key), timeout, unit);
    }

    @Override
    public List<String> range(String key, long start, long end) {
        return ops().range(wrap(key), start, end);
    }

    @Override
    public Long size(String key) {
        return ops().size(wrap(key));
    }

    @Override
    public String index(String key, long index) {
        return ops().index(wrap(key), index);
    }

    @Override
    public void set(String key, long index, String value) {
        ops().set(wrap(key), index, value);
    }

    @Override
    public Long remove(String key, long count, String value) {
        return ops().remove(wrap(key), count, value);
    }

    @Override
    public void trim(String key, long start, long end) {
        ops().trim(wrap(key), start, end);
    }
}
