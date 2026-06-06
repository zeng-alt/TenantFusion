package com.github.zeng.alt.storage.redisson;

import com.github.zeng.alt.storage.api.*;
import org.redisson.api.*;

import java.util.concurrent.TimeUnit;

/**
 * Redisson 实现 StorageTemplate
 * 基于 Redisson 分布式对象实现 String、List、Hash、ZSet 操作
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public class RedissonStorageTemplate extends AbstractStorageTemplate {

    private final RedissonClient redissonClient;

    private final CacheStringOperations stringOps;
    private final CacheListOperations listOps;
    private final CacheHashOperations hashOps;
    private final CacheZSetOperations zSetOps;

    public RedissonStorageTemplate(
            RedissonClient redissonClient,
            KeyPrefixStrategy keyPrefixStrategy,
            CacheStringOperations cacheStringOperations,
            CacheListOperations cacheListOperations,
            CacheHashOperations cacheHashOperations,
            CacheZSetOperations cacheZSetOperations
    ) {
        super(keyPrefixStrategy);
        this.redissonClient = redissonClient;
        this.stringOps = cacheStringOperations;
        this.listOps = cacheListOperations;
        this.hashOps = cacheHashOperations;
        this.zSetOps = cacheZSetOperations;
    }

    @Override
    public CacheStringOperations opsForString() {
        return stringOps;
    }

    @Override
    public CacheListOperations opsForList() {
        return listOps;
    }

    @Override
    public CacheHashOperations opsForHash() {
        return hashOps;
    }

    @Override
    public CacheZSetOperations opsForZSet() {
        return zSetOps;
    }

    @Override
    public Boolean delete(String key) {
        return redissonClient.getBucket(wrapKey(key)).delete();
    }

    @Override
    public Boolean hasKey(String key) {
        return redissonClient.getBucket(wrapKey(key)).isExists();
    }

    @Override
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redissonClient.getBucket(wrapKey(key)).expire(timeout, unit);
    }

    @Override
    public Long getExpire(String key) {
        RBucket<Object> bucket = redissonClient.getBucket(wrapKey(key));
        long ttl = bucket.remainTimeToLive();
        return ttl < 0 ? ttl : ttl / 1000;
    }
}
