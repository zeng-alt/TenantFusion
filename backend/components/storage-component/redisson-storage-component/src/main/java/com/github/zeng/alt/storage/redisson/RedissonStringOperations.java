package com.github.zeng.alt.storage.redisson;

import com.github.zeng.alt.storage.api.CacheStringOperations;
import com.github.zeng.alt.storage.api.KeyPrefixStrategy;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * Redisson String 结构操作实现
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public class RedissonStringOperations implements CacheStringOperations {

    private final RedissonClient redissonClient;
    private final KeyPrefixStrategy keyPrefixStrategy;

    public RedissonStringOperations(RedissonClient redissonClient, KeyPrefixStrategy keyPrefixStrategy) {
        this.redissonClient = redissonClient;
        this.keyPrefixStrategy = keyPrefixStrategy;
    }

    private String wrap(String key) {
        return keyPrefixStrategy.wrapKey(key);
    }

    @Override
    public void set(String key, String value) {
        redissonClient.getBucket(wrap(key)).set(value);
    }

    @Override
    public void set(String key, String value, long timeout, TimeUnit unit) {
        redissonClient.getBucket(wrap(key)).set(value, timeout, unit);
    }

    @Override
    public String get(String key) {
        return (String) redissonClient.getBucket(wrap(key)).get();
    }

    @Override
    public Boolean setIfAbsent(String key, String value) {
        return redissonClient.getBucket(wrap(key)).trySet(value);
    }

    @Override
    public Boolean setIfAbsent(String key, String value, long timeout, TimeUnit unit) {
        return redissonClient.getBucket(wrap(key)).trySet(value, timeout, unit);
    }

    @Override
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redissonClient.getBucket(wrap(key)).expire(timeout, unit);
    }

    @Override
    public Long getExpire(String key) {
        RBucket<Object> bucket = redissonClient.getBucket(wrap(key));
        long ttl = bucket.remainTimeToLive();
        return ttl < 0 ? ttl : ttl / 1000;
    }

    @Override
    public Boolean delete(String key) {
        return redissonClient.getBucket(wrap(key)).delete();
    }

    @Override
    public Boolean hasKey(String key) {
        return redissonClient.getBucket(wrap(key)).isExists();
    }

    @Override
    public Long increment(String key) {
        return redissonClient.getAtomicLong(wrap(key)).incrementAndGet();
    }

    @Override
    public Long increment(String key, long delta) {
        return redissonClient.getAtomicLong(wrap(key)).addAndGet(delta);
    }
}
