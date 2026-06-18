package com.github.zeng.alt.storage.redisson.suepr;

import com.github.zeng.alt.storage.CacheStringOperations;
import com.github.zeng.alt.storage.KeyPrefixStrategy;
import org.redisson.api.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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
        return keyPrefixStrategy.map(key);
    }

    @Override
    public <T> void set(String key, T value) {
        redissonClient.getBucket(wrap(key)).set(value);
    }

    @Override
    public <T> void set(String key, T value, Duration duration) {
        RBucket<T> bucket = redissonClient.getBucket(wrap(key));
        bucket.set(value, duration);
    }

    @Override
    public <T> T get(String key, Class<T> tClass) {
        RBucket<T> bucket = redissonClient.getBucket(wrap(key));
        return bucket.get();
    }

    @Override
    public <T> Boolean setIfAbsent(String key, T value) {
        return redissonClient.getBucket(wrap(key)).setIfAbsent(value);
    }

    @Override
    public <T> Boolean setIfAbsent(String key, T value, Duration duration) {
        return redissonClient.getBucket(wrap(key)).setIfAbsent(value, duration);
    }

    @Override
    public Boolean expire(String key, Duration duration) {
        return redissonClient.getBucket(wrap(key)).expire(duration);
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
    public long delete(String... keys) {

        if (keys == null || keys.length == 0) {
            return 0;
        }

        RBatch batch = redissonClient.createBatch();

        for (String key : keys) {
            batch.getBucket(wrap(key)).deleteAsync();
        }

        BatchResult<?> result = batch.execute();

        List<?> responses = result.getResponses();

        long count = 0;

        for (Object r : responses) {
            if (Boolean.TRUE.equals(r)) {
                count++;
            }
        }

        return count;
    }

    @Override
    public long deleteByPattern(String pattern) {

        RKeys keys = redissonClient.getKeys();

        Iterable<String> matchedKeys = keys.getKeysByPattern(wrap(pattern));

        List<String> keyList = new ArrayList<>();
        matchedKeys.forEach(keyList::add);

        if (keyList.isEmpty()) {
            return 0;
        }

        long count = 0;

        for (String key : keyList) {
            if (redissonClient.getBucket(key).delete()) {
                count++;
            }
        }
        return count;
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
