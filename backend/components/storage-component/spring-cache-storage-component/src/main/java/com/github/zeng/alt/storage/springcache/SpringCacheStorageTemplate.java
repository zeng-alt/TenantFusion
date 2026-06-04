package com.github.zeng.alt.storage.springcache;

import com.github.zeng.alt.storage.api.*;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Spring Data Redis 实现 StorageTemplate
 * 基于 StringRedisTemplate 实现 String、List、Hash、ZSet 操作
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public class SpringCacheStorageTemplate extends AbstractStorageTemplate {

    private final StringRedisTemplate redisTemplate;

    private final CacheStringOperations stringOps;
    private final CacheListOperations listOps;
    private final CacheHashOperations hashOps;
    private final CacheZSetOperations zSetOps;

    public SpringCacheStorageTemplate(StringRedisTemplate redisTemplate, KeyPrefixStrategy keyPrefixStrategy) {
        super(keyPrefixStrategy);
        this.redisTemplate = redisTemplate;
        this.stringOps = new SpringCacheStringOperations(redisTemplate, keyPrefixStrategy);
        this.listOps = new SpringCacheListOperations(redisTemplate, keyPrefixStrategy);
        this.hashOps = new SpringCacheHashOperations(redisTemplate, keyPrefixStrategy);
        this.zSetOps = new SpringCacheZSetOperations(redisTemplate, keyPrefixStrategy);
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
        return redisTemplate.delete(wrapKey(key));
    }

    @Override
    public Boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(wrapKey(key)));
    }

    @Override
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.expire(wrapKey(key), timeout, unit));
    }

    @Override
    public Long getExpire(String key) {
        return redisTemplate.getExpire(wrapKey(key));
    }
}
